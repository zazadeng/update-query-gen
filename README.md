# update-query-gen

A Clojure tool for data masking on a JDBC-friendly database.

##Problems trying to solve/explore
- Can we have an abstraction that performs all data masking activities for a particular table in a database?
- Can the abstraction be more human-friendly in term of readability and simplicity?

## EDN, Extensible Data Notation [eed-n]
EDN is used for crafting the wanted abstraction;
```clojure
{
 <SUBSYSTEM>_<SCHEMA>
  {
    :table <TABLE>
    :primary-key <KEY>
    :mask-columns {
            <COLUMN> {
                     :mask-set {
                                <REGULAR EXPRESSION> (<MASKING LOGIC IN CLOJURE>)
                                
                                }
                  }
            }
    :mask-select-query <SQL>
 }
}
````

## Example
1.suppose we run the following query:
```sql
select ADDRESSID, ADDRESSDATA, OWNER from ADDRESS
````

we have the following result(columns are seperated by "|"):

	1|ADD1=No.1 Ave \r ADD2=another address \r CITY=Richmond\r PROVINCE=BC \r COUNTRY=Canada|Zeyu Deng
	2|ADD1=No.3 Ave \r ADD2=another address \r CITY=Vancouver \r PROVINCE=BC \r COUNTRY=Canada|Kaka Deng

2.We now have an edn file(address.edn) specified as the following:
```clojure
{
  CMLY_CMSPROD
  {
    :table ADDRESS
    :primary-key ADDRESSID
    :mask-columns {
            ADDRESSDATA {
                    :mask-set {
                               "ADD1=[\\w ,.]+" (str "ADD1=Mask address" "_" :addressId)
                               "CITY=[\\w]+" (str "CITY=Mask city")
                               }
                  }
            OWNER {
                    :mask-set {"^.+$" (str "Mask owner" "_" (first :owner) "_" :addressId)}
                  }
             NOTEXIST {
                    :mask-set {"^.+$" (str "")}
                  } 
     }
    :mask-select-query "select ADDRESSID, ADDRESSDATA, OWNER from ADDRESS" ;fetch all keys
  }

  DSNY_PROD
  {
    :table ADDRESS
    :primary-key ADDRESSID
    :mask-columns {
            ADDRESSDATA {
                     :mask-set {
                                "ADD1=[\\w ,.]+" (dm-to-string "ADD1=Mask address")
                                "CITY=[\\w]+" (dm-to-string "CITY=Mask city")
                                }
                  }
            }
    :mask-select-query "select ADDRESSID, ADDRESSDATA from ADDRESS" ;fetch all keys
  }
 }
````

3.Calling a function to get an edn data set for updating a database which has subsystem named CMLY and schema named CMSPROD
```java
update_query_gen.core.getUpdateQueryDDL("./testdata/example.db", 
				"sqlite", "CMLY_CMSPROD", "./testdata/address.edn");
````

we will have the following result ready to take the database update activity:
{[ADDRESSID=? 2] {:owner Mask owner_K_2, :addressdata ADD1=Mask address_2 \r ADD2=another address \r CITY=Mask city \r PROVINCE=BC \r COUNTRY=Canada}, [ADDRESSID=? 1] {:owner Mask owner_Z_1, :addressdata ADD1=Mask address_1 \r ADD2=another address \r CITY=Mask city\r PROVINCE=BC \r COUNTRY=Canada}}

## License

Copyright © 2014 Zeyu(Zaza) Deng

Distributed under the Eclipse Public License, the same as Clojure.
