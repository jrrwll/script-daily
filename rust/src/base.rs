use std::fs;
use std::path::PathBuf;

pub trait DirWalker {
    fn is_verbose(&self) -> bool;

    fn is_abort(&self) -> bool;

    fn need_recursive(&self) -> bool {
        false
    }

    fn handle_dir_entry(&self, path: PathBuf) -> bool {
        true
    }

    fn handle_file_entry(&self, path: PathBuf) -> bool;

    fn handle_dir(&self, path: &PathBuf) -> bool {
        let Ok(read_dir) = fs::read_dir(path) else {
            eprintln!("failed to read directory {:?}", path);
            return false;
        };
        for dir_entry in read_dir {
            let dir_entry = match dir_entry {
                Ok(v) => v,
                Err(e) => {
                    if self.is_verbose() {
                        eprintln!("failed to read directory entry {:?}", e);
                    }
                    if self.is_abort() {
                        return false;
                    } else {
                        continue;
                    }
                }
            };
            let dir_entry_path = dir_entry.path();
            let Ok(meta) = dir_entry.metadata() else {
                if self.is_verbose() {
                    eprintln!("failed to get metadata {:?}", &dir_entry_path);
                }
                if self.is_abort() {
                    return false;
                } else {
                    continue;
                }
            };
            if meta.is_dir() {
                // handle children first
                if self.need_recursive() {
                    if !self.handle_dir(&dir_entry_path) {
                        if self.is_abort() {
                            return false;
                        }
                    }
                }
                // then handle itself
                if !self.handle_dir_entry(dir_entry_path) {
                    if self.is_abort() {
                        return false;
                    }
                }
            } else if meta.is_file() {
                if !self.handle_file_entry(dir_entry_path) {
                    if self.is_abort() {
                        return false;
                    }
                }
            }
        }
        true
    }
}
