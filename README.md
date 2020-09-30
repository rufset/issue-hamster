# issue-hamster
Just some code to fetch stuff. The app is made with Java 11, Springboot and Maven. The db is a mongodb. 

### To run in general you need

  - Java 11 
  - Mongo DB installed
  - Maven

Add directory in users home dir named Issue-hamster-files, this directory should contain two files
  - projects.txt containing a list of projects (on the form of user/project), one project per line
  - token.txt containing a personal access token for github, you can generate one under Settings -> personal settings -> personal access token. It is very important to not let the key loiter around. Don't check it in anywhere etc.



### To run locally on a mac you can

Use homebrew to download and install MongDB
Start up MongoDB locally in a terminal with
```sh
$ mongod --dbpath=<absolutePathToWhateverDirYouWantToPutDataIn>
``` 

in the root catalogue of the project open a console
Compile the project and get Spring magic to happen with

 ```sh
$ mvn clean verify
``` 

Add the dir and the files 

start the jar with: 
```sh
$ java -jar target/issue-hamster-0.0.1-SNAPSHOT.jar   
```

You can now log in to Swagger available, at localhost:8080 in a browser

To log in to swagger:<br>
Username user<br>
Password is generated at startup and is found after “Using generated security password:” in the log<br>

### Very specific instruction for usage on a AWS EC2 with Ubuntu 20.04

  - In AWS I picked a Free tier T2.micro with Ubuntu Focal 20.04
  - Instructions on how to log in to the server once it's started is found under "connect" on the instance page in AWS. Follow the instructions, save the key etc. 
  - For copying files you can use secure copy scp command:
```sh
$ scp -i "<the name of the pem-file, your key>" <file you want to copy> <ubuntu@blablablalbalba your computer.amazonaws.com:<the dir you want to save in, e.g. home /~ >
```
  - Install mongodb on ubuntu, i used [these](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/) instructions. The same instructions tell you how to start it as a service. This is necessary so it won't be killed when the terminal is terminated.
  - Install java,version 11.0.8+10-0ubuntu1~20.04:
```sh
$ sudo apt install openjdk-11-jre-headless  
```
  - Generate an ssh key and add to [github and your ubuntu machine] (https://www.inmotionhosting.com/support/website/ssh/how-to-add-ssh-keys-to-your-github-account/) accordingly
  - clone the issue-hamster project from github on the ubuntu machine
  - Install maven with
```sh
$ sudo apt install maven
```
  - step in to issue-hamster directive and run maven (as of now since the code is a command runner, make sure you don't have the files with all the projects bc it will run the thing in the verify stage. I should fix this. But being lazy. Anyhow this is to get all the spring boot magic loaded. (run the tests)
It will give you an error about a file missing. No worries.
 ```sh
$ mvn clean verify
``` 
  - If you haven't started mongodb already then now is the time to do it, make sure to start it as a service using the instructions above. You can check status of the db with:
 ```sh
$ sudo systemctl status mongod
``` 
  - Make sure you have added the token.txt and projects.txt-files in the Issue-hamster-files directory placed under user home directory. 
  - Run the jar file with nohup
 ```sh
$  nohup java -jar issue-hamster/target/issue-hamster-0.0.1-SNAPSHOT.jar &
``` 
 

