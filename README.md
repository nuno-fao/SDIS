# SDIS Project

SDIS Project for group T02G21.

Group members:

1. João Mascarenhas (up201806389@edu.fe.up.pt)
2. João Matos (up201703884@edu.fe.up.pt)
3. Luís Pinto(up201806206@edu.fe.up.pt)
4. Nuno Oliveira(up201806525@edu.fe.up.pt)



To help with compiling and running the project several scripts were created.  
These scripts can be found in the "scripts" directory which can be found at the root of the project tree, they're similar to the ones used in the first project.

Compilation:
To compile the project copy the compile.sh script to the root of the project tree and run it with no arguments, this will create a new "build" directory at the root of the project tree.

Cleanup:
If you would like to cleanup leftover files from previous executions copy the cleanup.sh script to the "build" directory and run it with no arguments.

Setup:
Before running the project copy the setup.sh script to the "build" directory and run it with no arguments (this will start the rmiregistry in the background). You don't need to run the script once for each peer, you only need to run it once.

Running the peers:
To run the peers copy the peer.sh script to the "build" directory. This script must be run in the following way (arguments between brackets are optional):
<path to script> <addr> <port> [<addr>:<port>] [<keystoreFile> <keystorePass> <truststoreFile> <keystorePass>]
The 2 mandatory arguments are the address (for example, "localhost" or "127.0.0.1") and port (for example, "6666") which will be used to communicate with that peer.
The first optional argument contains the address and port of a peer that is already running (already part of the Chord circle), if this argument isn't provided then a new Chord circle will be created. Note that the address and port are separated by ":", not a space, this is a single argument.
The second block contains several optional arguments. The "...File" arguments must be the paths to the respective files and the "...Pass" arguments must be the passwords to access the respective data. If these arguments aren't provided then the "keys" directory inside the "src" directory should be copied to the build directory.

Running the client:
To run the client copy the test.sh script to the "build" directory and run it in the following way:
<path to script> <peer id> <action> <args>*
The first argument is the ID of the peer that the client will contact. A peer's ID is printed on screen (the first line of output should look like this: "Peer: <id>") when a peer is initialized since Chord forces us to calculate the ID with a hashing function.
The second argument is the action that should be performed. There are 5 possible actions: BACKUP (for backing up a file), RESTORE (for restoring a file that was previously backed up), DELETE (for deleting from the service a file that has been previously backed up), RECLAIM (for setting a limit for the amount of storage that a peer can use, in kilobytes) and STATE (for getting information about the state of a peer).
The remaining arguments depend on the action being performed and should follow this structure:
BACKUP <path to file being backed up> <replication degree>
RESTORE <path to file being restored>
DELETE <path to file being deleted>
RECLAIM <storage limit in kilobytes>
(STATE has no arguments)

Explanation of a peer's directory tree:
When a peer runs it creates a directory whose name is the peer's ID, inside this directory can be found two subdirectories, one with stored files and another with restored files named, respectively, "stored" and "restored".  
These subdirectories will only be created if the peer has stored or restored any files.

