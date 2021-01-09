#stahnout
#wget -r -N --accept="zip,ZIP" ftp://ftp.cisjr.cz/draha/celostatni/szdc

#nebo stahnout jen konkretni rok
wget -r -N --accept="zip,ZIP" ftp://ftp.cisjr.cz/draha/celostatni/szdc/2021

java -Dfile.encoding=UTF-8 -jar GCRP.jar -o vlakyCR.zip -p test -g ftp.cisjr.cz/draha/celostatni/szdc -s 20201213 -e 20211213
