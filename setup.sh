#!/bin/bash

# UoB COMSM0016 DB VM setup script

# This is a rip-and-replace script - get confirmation before continuing...
read -p "DB VM setup: This will overwrite any existing DB VM and data! Continue? (y/n) " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then

  # Remove an existing VM
  echo "Removing any existing Virtual Machine..."
  vagrant destroy -f

  # Delete all plugins and reinstall
  vagrant plugin expunge --force --reinstall

  # Install vagrant Guest Extensions
  vagrant plugin install vagrant-vbguest

  # Force the download of a new Vagrant Box
  echo "Downloading Vagrant Box (this may take a few minutes)..."
  vagrant box add --clean --force --provider virtualbox bento/centos-7

  # Add mysql login defaults on PC
  echo "Setting PC mysql login defaults to ~/.my.cnf"
  cat << END1 > ~/.my.cnf
[client]
port=3306
host=127.0.0.1
user=student
END1

  # Deploy new VM
  echo "Building new database VM..."
  vagrant up --no-color

  # Note: final user info in Vagrantfile
  # End of if block
fi
