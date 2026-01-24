use rexl::argparse::{ArgParserRunnable, FromArgs};
use std::path::PathBuf;
use std::{env, fs};

use crate::base::DirWalker;
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, FromArgs)]
#[arg_parser(first_char)]
pub struct PoetryCli {
    pub authors: Vec<String>,
    pub titles: Vec<String>,
    pub contents: Vec<String>,

    pub abort: bool,
    #[arg_parser(name = "V")]
    pub verbose: bool,
}

#[derive(Debug, Serialize, Deserialize)]
struct Ci {
    author: Option<String>,
    paragraphs: Vec<String>,
    rhythmic: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct Shi {
    author: Option<String>,
    paragraphs: Vec<String>,
    title: Option<String>,
    biography: Option<String>,
    notes: Vec<String>,
    volume: Option<String>,
    #[serde(rename = "no#")]
    no: Option<String>,
}

trait Poetry: Serialize {

    fn author(&self) -> &Option<String>;

    fn paragraphs(&self) -> &Vec<String>;

    fn title(&self) -> &Option<String>;

    fn match_and_output(&self, args: &PoetryCli) {
        if !self.is_match(args) {
            return;
        }
        let output = serde_json::to_string_pretty(&self)
            .expect("failed to serialize json");
        println!("{}", output);
    }

    fn is_match(&self, args: &PoetryCli) -> bool {
        if !args.authors.is_empty() {
            if let Some(author) = self.author() {
                if !args.authors.iter().any(|s| author.contains(s)) {
                    return false;
                }
            }
        }
        if !args.titles.is_empty() {
            if let Some(title) = self.title() {
                if !args.titles.iter().any(|s| title.contains(s)) {
                    return false;
                }
            }
        }
        if !args.contents.is_empty() {
            let mut matched = false;
            for paragraph in self.paragraphs() {
                if args.contents.iter().any(|s| paragraph.contains(s)) {
                    matched = true;
                    break
                }
            }
            if !matched {
                return false;
            }
        }
        true
    }
}

impl Poetry for Ci {
    fn author(&self) -> &Option<String> {
        &self.author
    }

    fn paragraphs(&self) -> &Vec<String> {
        &self.paragraphs
    }

    fn title(&self) -> &Option<String> {
        &self.rhythmic
    }
}

impl Poetry for Shi {
    fn author(&self) -> &Option<String> {
        &self.author
    }

    fn paragraphs(&self) -> &Vec<String> {
        &self.paragraphs
    }

    fn title(&self) -> &Option<String> {
        &self.title
    }
}

impl ArgParserRunnable for PoetryCli {
    fn run(self) {
        let poetry_home = env::var("POETRY_HOME");
        let poetry_dir = match poetry_home.map(|v| PathBuf::from(v)) {
            Ok(v) => v,
            Err(_) => {
                let Some(v) = get_poetry_dir() else {
                    eprintln!("failed to get os home dir");
                    return;
                };
                v
            }
        };

        let ci_dir = poetry_dir.join("ci");
        let ci_ctx = CiCtx { args: self.clone() };
        ci_ctx.handle_dir(&ci_dir);

        let quan_tang_shi_dir = poetry_dir.join("quan_tang_shi").join("json");
        let quan_tang_shi_ctx = QuantangshiCtx { args: self.clone() };
        quan_tang_shi_ctx.handle_dir(&quan_tang_shi_dir);
    }
}

struct CiCtx {
    args: PoetryCli,
}

impl DirWalker for CiCtx {
    fn is_verbose(&self) -> bool {
        self.args.verbose
    }

    fn is_abort(&self) -> bool {
        self.args.abort
    }

    fn handle_file_entry(&self, path: PathBuf) -> bool {
        let list: Vec<Ci> = match parse_json_list(&path, |v| v.starts_with("ci.song") && v.ends_with(".json")) {
            Ok(v) => v,
            Err(result) => return result
        };
        for poetry in list {
            poetry.match_and_output(&self.args);
        }
        true
    }
}

struct QuantangshiCtx {
    args: PoetryCli,
}

impl DirWalker for QuantangshiCtx {
    fn is_verbose(&self) -> bool {
        self.args.verbose
    }

    fn is_abort(&self) -> bool {
        self.args.abort
    }

    fn handle_file_entry(&self, path: PathBuf) -> bool {
        let list: Vec<Shi> = match parse_json_list(&path, |v| v.ends_with(".json")) {
            Ok(v) => v,
            Err(result) => return result
        };
        for poetry in list {
            poetry.match_and_output(&self.args);
        }
        true
    }
}

fn parse_json_list<T: DeserializeOwned, F: Fn(String) -> bool>(
    path: &PathBuf,
    filter: F,
) -> Result<Vec<T>, bool> {
    let Some(file_name) = path.file_name().map(|v| v.to_string_lossy().to_string()) else {
        eprintln!("failed to get filename {:?}", &path);
        return Err(false);
    };
    if !filter(file_name) {
        return Err(true);
    }

    let file = match fs::File::open(&path) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("cannot open file {:?}: {}", &path, e);
            return Err(false);
        }
    };

    let list: Vec<T> = match serde_json::from_reader(file) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("failed to parse json file {:?}: {}", &path, e);
            return Err(false);
        }
    };
    Ok(list)
}

fn get_poetry_dir() -> Option<PathBuf> {
    dirs::home_dir().map(|home| home.join("hub").join("chinese-poetry"))
}
