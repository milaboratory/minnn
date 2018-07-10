============
Installation
============

#. To use mist, you need to install java version 8 or higher. For Mac OS X you can download it here:
   https://java.com/en/download/

   On Linux install it from repository of your distro. You can check that java is installed by typing in command line:

   .. code-block:: console

      java -version

   If everything is correct, it should display version 1.8 or higher.
#. Copy mist.jar into ~/bin directory, or you can use any other directory where you store binary executables.
#. Edit ~/.bash_profile (on Mac OS X) or ~/.bashrc (on Linux) and add the line:

   .. code-block:: console

      alias mist='java -jar ~/bin/mist.jar'

   Then open new terminal window and type:

   .. code-block:: console

      mist --help

   If everything is correct, mist help will be displayed.
