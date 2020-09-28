# issue-hamster
Just some code to fetch stuff

Create a catalogue in running users home dir named Issue-hamster-files
Place 2 files in dir<br>
projects.txt containing a list of projects (on the form of user/project), one project per line<br>
token.txt containing a personal access token to github in it. <br>
A subdir named data<br>

Install MongoDB <br>
Start up MongoDB with mongod --dbpath=<absolutePathTohomeDir>/Issue-hamster-files/data/<br>

in the root catalogue of the project open a console<br>
Compile the project and get Spring magic to happen: mvn clean verify <br>
start the jar with: java -jar target/issue-hamster-0.0.1-SNAPSHOT.jar 

