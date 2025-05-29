# How to Setup PostgreSQL Development

Install `postgresql`
Start `postgresql`

Confirm with:
```
pg_isready
```

Create a user called `myuser`:
```
createuser -s myuser
```

Compile and Run `CreatePostgresDB.java`
(if you are using VSCode & have the Java plugin you can simply press `Run` inside the file)

# Interacting with the DB

Run:

```
psql -U myuser -d my_database
```

`myuser` is the name of the user you created previously
`my_database` is the name of the database you created

List roles by doing:
```
\du
```

List databases by doing:
```
\l
```

Exit by:
```
\q
```