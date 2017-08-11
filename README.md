# Cppchecker plugin for jenkins
This [Jenkins CI](http://jenkins-ci.org/) plug-in execute [cppcheck](http://cppcheck.sourceforge.net/) with UI to configure parameters. [cppcheck](http://cppcheck.sourceforge.net/) is a tool for static C/C++ code analysis.

Following table show you how many cppcheck options were implemented in this plugin.

Option                                  | Status
------                                  | :----:
--append=\<file>                        |
--check-config                          |
--check-library                         |
--config-exclude=\<dir>                 |
--config-excludes-file=\<file>          |
--dump                                  | V
-D\<ID>                                 | V
-U\<ID>                                 |
-E                                      |
--enable=\<id>                          | V
--error-exitcode=\<n>                   |
--errorlist                             |
--exitcode-suppressions=\<file>         |
--file-list=\<file>                     |
-f, --force                             | V
-h, --help                              |
-I \<dir>                               | V
--includes-file=\<file>                 |
--include=\<file>                       |
-i \<dir or file>                       |
--inconclusive                          | V
--inline-suppr                          |
-j \<jobs>                              |
-l \<load>                              |
--language=\<language>, -x \<language>  |
--library=\<cfg>                        |
--max-configs=\<limit>                  |
--platform=\<type>, --platform=\<file>  |
-q, --quiet                             | V
-rp, --relative-paths                   |
-rp=\<paths>, --relative-paths=\<paths> |
--report-progress                       |
--rule=\<rule>                          |
--rule-file=\<file>                     |
--std=\<id>                             | V
--suppress=\<spec>                      | V
--suppressions-list=\<file>             |
--template='\<text>'                    |
-v, --verbose                           | V
--version                               |
--xml                                   | V
--xml-version=\<version>                | V
