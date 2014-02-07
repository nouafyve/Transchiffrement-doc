sudo -S apt-get install libnss3-tools
certificateFile="cert.pem"
certificateName="SSISign" 
IFS='';
find /home/$1/.mozilla/firefox -name "cert8.db" | while read file; do test=$(dirname $file); echo $test; certutil -A -n $certificateName -t "CT,C,C" -i $certificateFile -d $test; done

