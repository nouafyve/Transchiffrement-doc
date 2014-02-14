echo $#
if [ $# != 3 ]
then
	echo "Erreur, entrez ./install.sh root@IP rootPassword userName"
else
	victime=$1
	chemin=~/Téléchargements/script/
	password=$2
	sshpass -p "$password" scp $chemin/* $victime:~/
	echo "COPIE FINIE"
	sshpass -p "$password" ssh $victime -o RequestTTY=yes "chmod 755 del.sh; ./del.sh $3" 
fi
