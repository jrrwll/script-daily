pub mod base;
pub mod time;
pub mod rename;
pub mod json;
pub mod poetry;
pub mod mail;

use std::process;

use rexl::argparse::{run_with_args_tree, ArgParserRunnable, FromArgs, RunWithArgs};

use crate::json::{JsonCompareCli, JsonSortCli};
use crate::mail::MailSendCli;
use crate::poetry::PoetryCli;
use crate::rename::*;
use crate::time::*;

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
        "js,json_sort" => JsonSortCli,
        "jc,json_compare" => JsonCompareCli,
        "p,poetry" => PoetryCli,
        "m,mail" => MailSendCli,
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
