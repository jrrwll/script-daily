

function del_file_prefix() {
    if [ $# -lt 2 ]; then
        echo "Usage: del_file_prefix <dir> <prefix> <-y>"
        return 1
    fi

    dir=$1
    prefix=$2
    if [ ! -d $dir ]; then
        echo "dir $dir not exist"
        return 1;
    fi

    ls -a | grep $prefix | while read i; do
      new_name="${i#$prefix}"
      echo "mv $i $new_name"
      if [ "$3" = "-y" ]; then
        mv $i $new_name
      fi
    done
}