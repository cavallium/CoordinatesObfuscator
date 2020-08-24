WORKSPACE=".papermc"
MC_VERSION="1.16.2"
PAPER_BUILD="1"

## ============== DO NOT EDIT THE SCRIPT BELOW UNLESS YOU KNOW WHAT YOU ARE DOING ============== ##

#cd || exit # Moving to the user folder or exit if it fails.

[ -d $WORKSPACE ] || mkdir $WORKSPACE
[ -d $WORKSPACE ] || mkdir $WORKSPACE/plugins
cp target/*.jar $WORKSPACE/plugins || exit

# Checking the workspace folder availability.
if [ ! -d $WORKSPACE ]; then
  # Create the workspace folder.
  mkdir $WORKSPACE
fi

cd $WORKSPACE || exit # Moving to the workspace fodler or exit if it fails.

# Check for the paper executable
PAPER_JAR="paper-$MC_VERSION-$PAPER_BUILD.jar"
PAPER_LNK="https://papermc.io/api/v1/paper/$MC_VERSION/$PAPER_BUILD/download"

if [ ! -f $PAPER_JAR ]; then
  wget -O $PAPER_JAR $PAPER_LNK
fi

/usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar $PAPER_JAR nogui
