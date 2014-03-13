Windows
-------
On Windows, the binary is: instmon-win32-x86

ole32.lib is required for COM support
uuid.lib is required for COM identifiers (IID_IPersistFile)

Linux
-----
On Linux, the binary is: instmon-linux-x86

If CDT shows errors due to indexing problems, adjust the include paths in the
project settings: 
File>Properties>C/C++ General>Paths and Symbols>Includes>GNU C