#! /usr/bin/bash

# Placeholder for clean-up script
# To be executed in the root of the build tree
# Requires at most one argument: the peer id
# Cleans the directory tree for storing 
#  both the chunks and the restored files of
#  either a single peer, in which case you may or not use the argument
#    or for all peers, in which case you 


# Check number input arguments
argc=$#

if ((argc == 1 ))
then
	peer_id=$1
	if [ -d "${peer_id}_folder" ]; then
		rm "${peer_id}_folder" -r
  		echo "Removed files from peer ${peer_id}"
  	else 
  		echo "Folder ${peer_id}_folder not Found"
	fi
else 
	rm *_folder -r
  	echo "Removed files from evry peer"
fi

# Clean the directory tree for storing files
# For a crash course on shell commands check for example:
# Command line basi commands from GitLab Docs':	https://docs.gitlab.com/ee/gitlab-basics/command-line-commands.html
# For shell scripting try out the following tutorials of the Linux Documentation Project
# Bash Guide for Beginners: https://tldp.org/LDP/Bash-Beginners-Guide/html/index.html
# Advanced Bash Scripting: https://tldp.org/LDP/abs/html/

