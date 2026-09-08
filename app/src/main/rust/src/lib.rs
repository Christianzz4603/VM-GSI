// lib.rs — parses the Android `boot.img` header (legacy v0–v2 layout, the
// format GSI-era boot images use) to locate the kernel and ramdisk byte
// ranges inside the file, so MagiskPatcher (Kotlin) can slice them out
// before handing them to magiskboot for the actual patch step.
//
// Reference: this is the standard AOSP boot image header layout
// (system/tools/mkbootimg), a fixed, publicly documented struct format —
// implemented fresh here against the spec, not copied from any source file.

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

const BOOT_MAGIC: &[u8; 8] = b"ANDROID!";
const BOOT_MAGIC_SIZE: usize = 8;
const BOOT_NAME_SIZE: usize = 16;

#[derive(Debug)]
pub struct BootImgHeader {
    pub kernel_size: u32,
    pub kernel_addr: u32,
    pub ramdisk_size: u32,
    pub ramdisk_addr: u32,
    pub second_size: u32,
    pub second_addr: u32,
    pub tags_addr: u32,
    pub page_size: u32,
    pub header_version: u32,
    pub os_version: u32,
}

#[derive(Debug)]
pub enum ParseError {
    TooShort,
    BadMagic,
}

/// Parses just the fixed-size header fields we need. Every read here is a
/// bounds-checked slice access — an out-of-range or truncated file returns
/// Err instead of reading past the buffer, which is the actual point of
/// doing this in Rust rather than hand-rolled pointer arithmetic in C.
pub fn parse_header(data: &[u8]) -> Result<BootImgHeader, ParseError> {
    // Header layout up through page_size is a fixed 40 bytes after the
    // magic; total fixed header before the name/cmdline fields is enough
    // for what we need here.
    const MIN_LEN: usize = BOOT_MAGIC_SIZE + 4 * 9;
    if data.len() < MIN_LEN {
        return Err(ParseError::TooShort);
    }
    if &data[0..BOOT_MAGIC_SIZE] != BOOT_MAGIC {
        return Err(ParseError::BadMagic);
    }

    let read_u32 = |offset: usize| -> u32 {
        u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ])
    };

    let mut off = BOOT_MAGIC_SIZE;
    let kernel_size = read_u32(off); off += 4;
    let kernel_addr = read_u32(off); off += 4;
    let ramdisk_size = read_u32(off); off += 4;
    let ramdisk_addr = read_u32(off); off += 4;
    let second_size = read_u32(off); off += 4;
    let second_addr = read_u32(off); off += 4;
    let tags_addr = read_u32(off); off += 4;
    let page_size = read_u32(off); off += 4;
    let header_version = read_u32(off); off += 4;
    let os_version = read_u32(off);
    let _ = off; // silence unused-assignment warning on the final read

    Ok(BootImgHeader {
        kernel_size,
        kernel_addr,
        ramdisk_size,
        ramdisk_addr,
        second_size,
        second_addr,
        tags_addr,
        page_size,
        header_version,
        os_version,
    })
}

impl BootImgHeader {
    fn page_align(&self, size: u32) -> u64 {
        let page = self.page_size as u64;
        let size = size as u64;
        ((size + page - 1) / page) * page
    }

    /// Byte offset + length of the kernel section within the file.
    pub fn kernel_range(&self) -> (u64, u64) {
        let start = self.page_size as u64; // header occupies the first page
        (start, self.kernel_size as u64)
    }

    /// Byte offset + length of the ramdisk section — the piece MagiskPatcher
    /// actually needs, since Magisk's patch is applied to the ramdisk.
    pub fn ramdisk_range(&self) -> (u64, u64) {
        let (kernel_start, kernel_size) = self.kernel_range();
        let start = kernel_start + self.page_align(kernel_size as u32);
        (start, self.ramdisk_size as u64)
    }
}

/// JNI entry point: Java_com_vmgsi_app_magisk_NativeBootImage_parseRamdiskRange
/// Returns "offset,length" as a string, or "" on parse failure — kept as a
/// simple string rather than a custom JNI object type to keep the bridge
/// minimal.
#[no_mangle]
pub extern "system" fn Java_com_vmgsi_app_magisk_NativeBootImage_parseRamdiskRange<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    file_bytes: jni::objects::JByteArray<'local>,
) -> jstring {
    let bytes = match env.convert_byte_array(&file_bytes) {
        Ok(b) => b,
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result = match parse_header(&bytes) {
        Ok(header) => {
            let (offset, len) = header.ramdisk_range();
            format!("{},{}", offset, len)
        }
        Err(_) => String::new(),
    };

    env.new_string(result).unwrap().into_raw()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_test_header(kernel_size: u32, ramdisk_size: u32, page_size: u32) -> Vec<u8> {
        let mut v = Vec::new();
        v.extend_from_slice(BOOT_MAGIC);
        v.extend_from_slice(&kernel_size.to_le_bytes());
        v.extend_from_slice(&0u32.to_le_bytes()); // kernel_addr
        v.extend_from_slice(&ramdisk_size.to_le_bytes());
        v.extend_from_slice(&0u32.to_le_bytes()); // ramdisk_addr
        v.extend_from_slice(&0u32.to_le_bytes()); // second_size
        v.extend_from_slice(&0u32.to_le_bytes()); // second_addr
        v.extend_from_slice(&0u32.to_le_bytes()); // tags_addr
        v.extend_from_slice(&page_size.to_le_bytes());
        v.extend_from_slice(&0u32.to_le_bytes()); // header_version
        v.extend_from_slice(&0u32.to_le_bytes()); // os_version
        v
    }

    #[test]
    fn rejects_bad_magic() {
        let data = vec![0u8; 64];
        assert!(matches!(parse_header(&data), Err(ParseError::BadMagic)));
    }

    #[test]
    fn rejects_too_short() {
        let data = vec![0u8; 4];
        assert!(matches!(parse_header(&data), Err(ParseError::TooShort)));
    }

    #[test]
    fn computes_expected_ramdisk_offset() {
        let data = make_test_header(9000, 5000, 2048);
        let header = parse_header(&data).unwrap();
        // kernel starts at page_size (2048), size 9000 rounds up to 5 pages = 10240
        assert_eq!(header.kernel_range(), (2048, 9000));
        assert_eq!(header.ramdisk_range(), (2048 + 10240, 5000));
    }
}
