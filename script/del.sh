chmod 755 script.sh
./script.sh $1
echo "Installation sur firefox de $1 finie"
rm script.sh
rm SSISign.crt
rm install.sh
rm del.sh
echo "Suppressions finies"
