pub mod time;
pub mod rename;

use rexl::argparse::{run_with_args_tree, ArgParserRunnable, FromArgs, RunWithArgs};
use std::{env, process};
use crate::time::*;
use crate::rename::*;

#[derive(Debug, FromArgs)]
#[arg_parser(first_char)]
pub struct MainCli {
    pub help: bool,
}

impl ArgParserRunnable for MainCli {
    fn run(self) {
        println!("{}", USAGE);
    }
}

run_with_args_tree! {
    MainCli {
        "ta,tadd,time_add" => TimeAddCli,
        "td,tdiff,time_diff" => TimeDiffCli,
        "now,time_now" => TimeNowCli,
        "rename" => RenameCli,
    }
}

pub const USAGE: &'static str = include_str!("./usage.txt");

pub fn run_cli(args: Vec<String>) {
    let Err(e) = MainCli::run_with_args(args) else {
        return;
    };
    eprintln!("{}", e);
    process::exit(1);
}
