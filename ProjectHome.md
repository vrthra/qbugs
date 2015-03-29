A command line shell for bugzilla and similar bug tracking systems. (Public Domain)
Only bugster adapter is currently available.

See https://code.google.com/p/qbugs/source/browse/trunk/shell/README for tutorial.

example usage:

  * Creating a few commands
```
category -in utility |subcategory -in squid    } squid
category -in utility |subcategory -in apache   } apache
responsibleengineer -eq me.myemail@sun.com } allmybugs
responsibleengineer -eq me.myemail@sun.com | status -notin 11,10 } mybugs
```

  * Using one of the new commands to query bugs from bugster
```
    | responsibleengineer -eq me.myemail@sun.com
    >6609048 Apache 2.2.x rejects Solaris LDAP
    >6613260 Integration CR for squid into OpenSolaris
```
  * Using a new command as part of a pipeline (Last one creates a newer command)
```
    | responsibleengineer -eq me.myemail@sun.com |status -notin 11,10
    | responsibleengineer -eq me.myemail@sun.com |status -notin 11,10 |priority -in 1,2
    | responsibleengineer -eq me.myemail@sun.com |status -notin 11,10 |priority 1,2 } mybigbugs
```
  * Invoke the last created command
```
    | mybigbugs |category -in utility |subcategory -in squid
```