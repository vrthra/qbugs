SHELL=/bin/zsh
JAVA_HOME=/share/builds/components/jdk/1.5.0/SunOS/
ZH=$(PWD)
JARS=$(ZH)/deps/jline.jar:$(ZH)/resources/lib/jaxb-rt-1.0-ea.jar:$(ZH)/resources/lib/activation.jar:$(ZH)/resources/lib/bt-client.jar:$(ZH)/resources/lib/dom.jar:$(ZH)/resources/lib/dom4j.jar:$(ZH)/resources/lib/jaxm-runtime.jar:$(ZH)/resources/lib/jaxm-api.jar:$(ZH)/resources/lib/saaj-api.jar:$(ZH)/resources/lib/mail.jar:$(ZH)/resources/lib/saaj-impl.jar:$(ZH)/resources/lib/servlet.jar:$(ZH)/resources/lib/util.jar:$(ZH)/resources/lib/wsf-api.jar:$(ZH)/resources/lib/xercesImpl.jar

CP=$(ZH)/build:$(JAVA_HOME)/lib/tools.jar:$(JAVA_HOME)/lib/dt.jar:$(JARS)

all: jar
	@echo "done"

compile:
	mkdir -p build;
	(cd build; $(JAVA_HOME)/bin/javac -d . -classpath $(CP) ../src/**/*.java)

jar: compile
	(cd build; jar -cf ../zbug.jar ./**/*.class ../src/*.java ../zbug ../README)

clean:
	rm zbug.jar
	rm -rf build
