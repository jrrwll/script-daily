mod time;

use rexl::argparse::{run_with_args_tree, ArgParserRunnable, FromArgs, RunWithArgs};
use std::env;
use crate::time::*;

#[derive(Debug, FromArgs)]
pub struct MainCli {
    pub help: bool,
}

impl ArgParserRunnable for MainCli {
    fn run(self) {
        println!("{:?}", self);
    }
}

run_with_args_tree! {
    MainCli {
        "ta,tadd,time_add" => TimeAddCli,
        "td,tdiff,time_diff" => TimeDiffCli,
        "now,time_now" => TimeNowCli,
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = env::args().skip(1).collect();
    MainCli::run_with_args(args).expect("failed to parse args");
    Ok(())
}
