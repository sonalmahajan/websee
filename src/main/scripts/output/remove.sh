recurse() {
  for i in "$1"/*;do
    if [ -d "$i" ];then
      rm "$i/diff_oracle_test.txt"
      rm "$i/filtered_diff_oracle_test.txt"
      rm "$i/test.png"
      recurse "$i"
    fi
  done
}

recurse "."
