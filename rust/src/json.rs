use std::fs;
use serde_json::Value;

use rexl::argparse::{ArgParserRunnable, FromArgs};

#[derive(Debug, FromArgs)]
pub struct JsonSortCli {
    #[arg_parser(position = 0)]
    pub file: String,
    #[arg_parser(name = "d")]
    pub desc: bool,
}

impl ArgParserRunnable for JsonSortCli {

    fn run(self) {
        let file = match fs::File::open(&self.file) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot open file {}: {}", &self.file, e);
                return;
            },
        };
        let value: Value = match serde_json::from_reader(file) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot parse json from left_file {}: {}", &self.file, e);
                return;
            },
        };
        todo!()
    }
}

#[derive(Debug, FromArgs)]
pub struct JsonCompareCli {
    #[arg_parser(position = 0)]
    pub left_file: String,
    #[arg_parser(position = 1)]
    pub right_file: String,
}

impl ArgParserRunnable for JsonCompareCli {

    fn run(self) {
        let file1 = match fs::File::open(&self.left_file) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot open left_file {}: {}", &self.left_file, e);
                return;
            },
        };
        let file2 = match fs::File::open(&self.right_file) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot open right_file {}: {}", &self.right_file, e);
                return;
            },
        };

        let value1: Value = match serde_json::from_reader(file1) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot parse json from left_file {}: {}", &self.left_file, e);
                return;
            },
        };
        let value2: Value = match serde_json::from_reader(file2) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("cannot parse json from right_file {}: {}", &self.right_file, e);
                return;
            },
        };
        todo!()
    }
}

