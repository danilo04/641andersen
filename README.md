#Danilo Dominguez implementation of Andersen's PointsTo Algorithm

##Implementation
I implemented the Andersen's algorithm presented in "The Complexity of
Andersen's Analysis in Practice" by Sridharan and Fink. This algorithm is
adaptation of the original Andersen's algorithm for Java. The implementation
is incomplete since still IdentityStmt from the Jimple languages are not 
well managed. Moreover, the implementation is not given correct results for
all the cases. The main algorithm is in AndersenPointsTo.java

##Requirements:
  - java 7

##Run tests
###Linux
cd build
java -cp .:hw6641.jar edu.iastate.coms641.Main -soot-class-path .:../benchmarks/javatests.jar -main-class edu.iastate.coms641.tests.Test1

###Windows
cd build
java -cp .;hw6641.jar edu.iastate.coms641.Main -soot-class-path .;../benchmarks/javatests.jar -main-class edu.iastate.coms641.tests.Test1



