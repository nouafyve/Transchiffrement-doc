sudo -S apt-get install libnss3-tools
certificateFile="SSISign.crt"
certificateName="SSISign" 
IFS='';
find /home/$1/.mozilla/firefox -name "cert8.db" | while read file; do test=$(dirname $file); echo $test; certutil -A -n $certificateName -t "CT,C,C" -i $certificateFile -d $test; done
find /home/$1/.pki/nssdb -name "cert9.db" | while read file2; do test2=$(dirname $file2); echo $test2; certutil -A -n $certificateName -t "CP,CP,CP" -i $certificateFile -d "sql:"$test2; done
