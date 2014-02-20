echo $#
if [ $# != 3 ]
then
	echo "Erreur, entrez ./install.sh root@IP rootPassword userName"
else
	victime=$1
	chemin=~/Transchiffrement-doc/script/
	password=$2
	sshpass -p "$password" scp -oStrictHostKeyChecking=no $chemin/* $victime:~/ 
	echo "COPIE FINIE"
	sshpass -p "$password" ssh $victime -o RequestTTY=yes "chmod 755 del.sh; ./del.sh $3" 
fi
