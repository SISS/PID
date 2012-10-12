dbname=""
if [ -z $1 ] ; then
        dbname="pidsvc"
else
        dbname=$1
fi

#sudo su - postgres || exit 1
createuser pidsvc-admin -P || exit 1
createdb $dbname -O pidsvc-admin || exit 1
createlang plpgsql $dbname || exit 1
psql -d $dbname -f postgresql.sql || exit 1
