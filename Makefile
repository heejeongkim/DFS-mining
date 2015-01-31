JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Pattern.java \
	ValueComparator.java \
	NewAFP.java

default: classes


classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
