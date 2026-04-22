use std::env;

use rstool::run_cli;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = env::args().skip(1).collect();
    run_cli(args);
    Ok(())
}
