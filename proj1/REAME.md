IMPORTANT: Version 15 of jdk must be used, the code was tested with openjdk15.
Versions below that may not work

Compile: run ./scripts/compile.sh on src/ directory

The other scripts shall be initiated on the build sub-directory

Each server creates a [0-9]*_folder where it stores the metadata. 
The recovered files are stored under the folder "restored" on the respective peer folder

rmiregistry shall be initiated before running the peers

