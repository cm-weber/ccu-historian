copy ..\config\sim-sample.h2.db sim.h2.db
del out.log.*
java -jar ..\..\ccu-historian\build\ccu-historian.jar -config ..\config\sim.config
