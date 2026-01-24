use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::PathBuf;

use regex::Regex;
use rexl::argparse::{ArgParserRunnable, FromArgs};
use rexl::text::translate;

use crate::base::DirWalker;

#[derive(Debug, FromArgs)]
pub struct RenameCli {
    #[arg_parser(position = 0)]
    pub source_path: Option<String>,

    // full match to the filename
    #[arg_parser(name = "s,sr", from_str)]
    pub source_regex: Vec<Regex>,
    // --r regex1 replacement1 regex2 replacement2 ..., regex like: ^(.+)\?.+$, replacement like: $1
    #[arg_parser(name = "r,rr")]
    pub replacement_regex: Vec<String>,

    #[arg_parser(name = "t")]
    pub types: Vec<String>, // f, file, d, dir
    #[arg_parser(name = "R")]
    pub recursive: bool,
    #[arg_parser(name = "trim")]
    pub trim_char: String,
    pub trim_str: Vec<String>,
    // translate a char to another char: --tr '&_' '#_' '$_'
    #[arg_parser(name = "tr")]
    pub tr_char: Vec<String>,

    #[arg_parser(name = "f")]
    pub force: bool,
    pub abort: bool,
    #[arg_parser(name = "y")]
    pub yes: bool,
    #[arg_parser(name = "V")]
    pub verbose: bool,
}

impl ArgParserRunnable for RenameCli {
    fn run(self) {
        let Some((source_path, ctx)) = self.parse_ctx() else {
            return;
        };

        ctx.handle_dir(&source_path);
    }
}

impl RenameCli {
    fn parse_ctx(self) -> Option<(PathBuf, RenameCtx)> {
        if self.replacement_regex.is_empty()
            && self.trim_char.is_empty()
            && self.trim_str.is_empty()
            && self.tr_char.is_empty()
        {
            eprintln!("required one of --replacement-regex | --trim-char | --trim-str| --tr-char");
            return None;
        }
        if self.replacement_regex.len() / 2 != 0 {
            eprintln!("invalid length for --replacement-regex, only even arg values is supported");
            return None;
        }
        for c in &self.tr_char {
            if c.len() != 2 {
                eprintln!("invalid value `{}` for --tr", c);
                return None;
            }
        }

        let source_path = self.source_path.clone().unwrap_or(".".to_string());
        let Ok(source_path) = fs::canonicalize(&source_path) else {
            eprintln!("{} is not a directory", source_path);
            return None;
        };

        let types: HashSet<String> = self.types.iter().map(|s| s.to_lowercase()).collect();
        let (include_file, include_dir) = if types.len() == 0 {
            (true, false)
        } else {
            (
                types.contains("f") || types.contains("file"),
                types.contains("d") || types.contains("dir"),
            )
        };

        let trim_chars: Vec<char> = if !self.trim_char.is_empty() {
            self.trim_char.chars().collect()
        } else {
            vec![]
        };
        let tr_chars: HashMap<char, char> = self.tr_char.iter()
            .map(|s| {
                let mut chars = s.chars();
                (chars.next().unwrap(), chars.next().unwrap())
            })
            .collect();

        let mut replacement_regex = vec![];
        for chunk in self.replacement_regex.chunks_exact(2) {
            let Ok(pattern) = Regex::new(&chunk[0]) else {
                eprintln!("invalid regex `{}` for --replacement-regex", &chunk[0]);
                return None;
            };
            let replacement = chunk[1].clone();
            replacement_regex.push((pattern, replacement));
        };

        Some((
            source_path,
            RenameCtx {
                args: self,
                include_file,
                include_dir,
                trim_chars,
                tr_chars,
                replacement_regex,
            },
        ))
    }
}

struct RenameCtx {
    args: RenameCli,
    include_file: bool,
    include_dir: bool,
    trim_chars: Vec<char>,
    tr_chars: HashMap<char, char>,
    replacement_regex: Vec<(Regex, String)>,
}

impl DirWalker for RenameCtx {
    fn is_verbose(&self) -> bool {
        self.args.verbose
    }

    fn is_abort(&self) -> bool {
        self.args.abort
    }

    fn need_recursive(&self) -> bool {
        self.args.recursive
    }

    fn handle_dir_entry(&self, path: PathBuf) -> bool {
        self.handle_entry(path)
    }

    fn handle_file_entry(&self, path: PathBuf) -> bool {
        self.handle_entry(path)
    }
}

impl RenameCtx {

    fn handle_entry(&self, path: PathBuf) -> bool {
        if self.args.verbose {
            println!("start to handle {:?}", &path);
        }
        let Some(source_name) = path.file_name().map(|v| v.to_string_lossy().to_string()) else {
            eprintln!("failed to get filename {:?}", &path);
            return false;
        };
        if self.is_match_source_regex(&source_name) {
            if self.args.verbose {
                println!("unmatched source pattern, skip it: {:?}", &path);
            }
            return true;
        }

        let mut target_name = source_name.clone();

        if !self.trim_chars.is_empty() {
            target_name = target_name.trim_matches(self.trim_chars.as_slice()).to_string();
        }
        for s in &self.args.trim_str {
            match target_name.strip_prefix(s) {
                Some(v) => target_name = v.to_string(),
                None => {},
            };
        }

        if !self.tr_chars.is_empty() {
            target_name = translate(&target_name, &self.tr_chars);
        }

        for (pattern, replacement) in &self.replacement_regex {
            target_name = pattern.replace(&target_name, replacement).to_string();
        }

        if &source_name == &target_name {
            println!("same name between source and target, name: {}, source: {:?}",
                     &source_name, &path);
            return false;
        }

        let target_path = path.parent().unwrap().join(target_name);
        if target_path.exists() {
            if self.args.force {
                println!("delete target file {:?}", &target_path);
                if self.args.yes {
                    match fs::remove_file(&target_path) {
                        Ok(_) => {},
                        Err(e) => {
                            eprintln!("failed to delete target file {:?}, error: {}", &target_path, e);
                            return false;
                        }
                    };
                }
            } else {
                println!("file {:?} already exists in the target", &target_path);
                return true;
            }
        }
        println!("rename file {:?} to {:?}", &path, &target_path);
        if self.args.yes {
            match fs::rename(&path, &target_path) {
                Ok(_) => true,
                Err(e) => {
                    eprintln!("failed to rename file {:?} to {:?}, error: {}", &path, &target_path, e);
                    false
                }
            }
        } else {
            true
        }
    }

    fn is_match_source_regex(&self, source_name: &str) -> bool {
        if self.args.source_regex.is_empty() {
            return true;
        }
        for pattern in &self.args.source_regex {
            if pattern.is_match(source_name) {
                return true;
            }
        }
        false
    }
}
