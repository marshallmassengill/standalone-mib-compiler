# Standalone MIB to Events Compiler CLI

A standalone command-line application that converts SNMP MIB trap/notification definitions into OpenNMS event definitions. This tool extracts the core functionality from the OpenNMS MIB Compiler and packages it as a portable CLI application.

## Overview

This CLI application parses SNMP MIB files and generates OpenNMS-compatible event XML configurations from TRAP-TYPE and NOTIFICATION-TYPE definitions. It's designed for network administrators and OpenNMS users who need to quickly convert vendor MIB files into event definitions for SNMP trap processing.

## Features

### Core Functionality
- **MIB Parsing**: Uses JSMIParser to parse standard MIB file formats (`.mib`, `.txt`, `.my`)
- **Event Generation**: Converts TRAP-TYPE and NOTIFICATION-TYPE definitions into OpenNMS events
- **Dependency Resolution**: Automatically resolves MIB dependencies from specified directories
- **Enum Decoding**: Generates variable binding decode sections for INTEGER fields with named values

### CLI Features
- Process individual MIB files or entire directories
- Configurable UEI (Unique Event Identifier) base for generated events
- Output to file or stdout with formatted XML
- Individual event files per MIB or combined output
- Verbose logging for troubleshooting
- Comprehensive error reporting and validation

### Generated Event Features
- Unique Event Identifiers (UEIs) based on trap names
- Descriptive event labels and documentation
- SNMP trap masks for proper trap matching (enterprise ID, generic=6, specific type)
- Parameter substitution in log messages (`%parm[#1]%`, etc.)
- Variable binding decode for enumerated values
- HTML-formatted descriptions with parameter tables

## Requirements

- Java 11 or higher
- MIB files with TRAP-TYPE or NOTIFICATION-TYPE definitions
- Standard MIB dependencies (SNMPv2-SMI, SNMPv2-TC, etc.) when processing complex MIBs

## Building

```bash
# Build the standalone JAR
mvn clean package

# The executable JAR will be created at:
# target/standalone-mib-compiler-1.0.0-SNAPSHOT.jar
```

## Installation

No installation required. The generated JAR file is self-contained with all dependencies.

## Usage

### Basic Usage

```bash
# Process a single MIB file (output to stdout)
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar example.mib

# Process a single MIB file with output to file
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar -o events.xml example.mib

# Process all MIB files in a directory (combined output)
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar --process-all /path/to/mib-directory/

# Process all MIB files and create individual event files per MIB
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar --process-all --individual-files /path/to/mib-directory/
```

### Advanced Usage

```bash
# Custom UEI base for generated events
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  -u "uei.example.org/network-devices" \
  vendor-device.mib

# Specify dependency directory for standard MIBs
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  -d /usr/share/snmp/mibs/ \
  -o events.xml \
  vendor-specific.mib

# Process directory with verbose logging
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  --process-all \
  --verbose \
  -o all-network-events.xml \
  /path/to/network-mibs/

# Create individual event files with custom output directory
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  --process-all \
  --individual-files \
  -o /output/directory/ \
  /path/to/network-mibs/

# Individual files with custom UEI base
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  --process-all \
  --individual-files \
  -u "uei.company.org/network" \
  -o ./events/ \
  /path/to/network-mibs/
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `-d, --mib-directory DIR` | Directory containing MIB dependencies | Input file's directory |
| `-o, --output FILE` | Output file for events XML (or directory with `--individual-files`) | stdout |
| `-u, --uei-base BASE` | Base UEI for generated events | `uei.opennms.org/mib` |
| `-v, --verbose` | Enable verbose logging | false |
| `--process-all` | Process all MIB files in directory | false |
| `--individual-files` | Create separate event files for each MIB (requires `--process-all`) | false |
| `-h, --help` | Show help message | - |
| `-V, --version` | Show version information | - |

## Output Modes

The tool supports two output modes when processing multiple MIB files:

### Combined Output (Default)
When using `--process-all` without `--individual-files`, all events from all MIB files are combined into a single XML file. This is useful when you want a single event definition file for your OpenNMS configuration.

### Individual File Output
When using `--process-all` with `--individual-files`, each MIB file generates its own event XML file. Output files are named based on the input MIB filename:

- Input: `RFC1213-MIB.mib` → Output: `RFC1213-MIB-events.xml`
- Input: `IF-MIB.txt` → Output: `IF-MIB-events.xml`
- Input: `CISCO-DEVICE` → Output: `CISCO-DEVICE-events.xml`

The output directory can be specified with `-o`:
- If `-o` is not specified, files are written to the current directory
- If `-o` specifies a directory, files are written to that directory
- If `-o` specifies a file path, its parent directory is used

This mode is useful when:
- Managing event definitions separately for each vendor or device type
- Importing events selectively into OpenNMS
- Organizing large MIB collections

## Output Format

### Generated Event Structure

The tool generates OpenNMS-compatible event XML with the following structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<events>
  <event>
    <uei>uei.opennms.org/mib/systemFailure</uei>
    <event-label>VENDOR-MIB defined trap event: systemFailure</event-label>
    <descr>
      <p>System has experienced a critical failure</p>
      <table>
        <tr><td><b>deviceName</b></td><td>%parm[#1]%</td><td><p></p></td></tr>
        <tr><td><b>errorCode</b></td><td>%parm[#2]%</td><td><p>
          normal(1)
          warning(2)  
          critical(3)
        </p></td></tr>
      </table>
    </descr>
    <logmsg dest="logndisplay">
      <p>
        systemFailure trap received
        deviceName=%parm[#1]%
        errorCode=%parm[#2]%
      </p>
    </logmsg>
    <severity>Indeterminate</severity>
    <mask>
      <maskelement>
        <mename>id</mename>
        <mevalue>.1.3.6.1.4.1.12345</mevalue>
      </maskelement>
      <maskelement>
        <mename>generic</mename>
        <mevalue>6</mevalue>
      </maskelement>
      <maskelement>
        <mename>specific</mename>
        <mevalue>1</mevalue>
      </maskelement>
    </mask>
    <varbindsdecode>
      <parmid>parm[#2]</parmid>
      <decode varbindvalue="1" varbinddecodedstring="normal"/>
      <decode varbindvalue="2" varbinddecodedstring="warning"/>
      <decode varbindvalue="3" varbinddecodedstring="critical"/>
    </varbindsdecode>
  </event>
</events>
```

### Key Elements Explained

- **UEI**: Unique identifier combining base UEI and trap name
- **Event Label**: Human-readable label showing MIB name and trap name
- **Description**: HTML-formatted description with parameter documentation
- **Log Message**: Template for log entries with parameter substitution
- **Mask Elements**: SNMP trap matching criteria (enterprise ID, generic=6, specific trap number)
- **Variable Bindings Decode**: Enumeration value translations for INTEGER parameters

## Examples

### Example 1: Simple MIB Processing

```bash
# Create a simple test MIB
cat > device-alerts.mib << 'EOF'
DEVICE-ALERTS-MIB DEFINITIONS ::= BEGIN

deviceAlerts OBJECT IDENTIFIER ::= { 1 3 6 1 4 1 99999 }

deviceName OBJECT-TYPE
    SYNTAX OCTET STRING
    ACCESS read-only
    STATUS mandatory
    DESCRIPTION "Name of the device"
    ::= { deviceAlerts 1 }

powerFailure TRAP-TYPE
    ENTERPRISE deviceAlerts
    VARIABLES { deviceName }
    DESCRIPTION "Device has lost power"
    ::= 1

END
EOF

# Process the MIB
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar device-alerts.mib
```

### Example 2: Batch Processing (Combined Output)

```bash
# Process all MIBs in a directory with custom configuration
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  --process-all \
  --verbose \
  -u "uei.company.org/network" \
  -o network-events.xml \
  /opt/mibs/network-devices/

# Output shows processing results:
# Processing MIB file: cisco-device.mib
# Generated 5 events
# Processing MIB file: juniper-alerts.mib
# Generated 12 events
# Events written to: network-events.xml
# Summary:
#   Processed: 2 MIB files
#   Errors: 0 MIB files
#   Total events generated: 17
```

### Example 3: Batch Processing (Individual Files)

```bash
# Process all MIBs and create individual event files
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar \
  --process-all \
  --individual-files \
  -u "uei.company.org/network" \
  -o ./network-events/ \
  /opt/mibs/network-devices/

# Output shows processing results:
# Writing individual event files to: /path/to/network-events
# Processing MIB file: cisco-device.mib
#   Generated 5 events -> cisco-device-events.xml
# Processing MIB file: juniper-alerts.mib
#   Generated 12 events -> juniper-alerts-events.xml
# Summary:
#   Processed: 2 MIB files
#   Errors: 0 MIB files

# Generated files:
# network-events/cisco-device-events.xml
# network-events/juniper-alerts-events.xml
```

## Troubleshooting

### Common Issues

**"Cannot find module" errors:**
- Ensure standard MIBs (SNMPv2-SMI, SNMPv2-TC) are available in the dependency directory
- Use `-d` option to specify a directory containing required MIB dependencies
- Check that all imported MIBs are present with correct filenames

**"No events generated" message:**
- Verify the MIB contains TRAP-TYPE or NOTIFICATION-TYPE definitions
- Use `-v` flag to see detailed parsing information
- Some MIBs only contain OBJECT-TYPE definitions for data collection (not events)

**Parsing errors:**
- Ensure MIB file syntax is correct
- Try processing with `-v` flag to see detailed error messages
- Check that the MIB file encoding is UTF-8 or ASCII

### Validation Example

The repository includes a validation test that demonstrates all features:

```bash
# Run the included validation test
java -jar standalone-mib-compiler-1.0.0-SNAPSHOT.jar validation-test.mib

# This generates events showing:
# - Basic trap processing  
# - Parameter substitution
# - Enum value decoding
# - Proper XML formatting
```

## Exit Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `0` | Success | All MIB files processed successfully |
| `1` | Error | General error (invalid arguments, file not found, parsing failure) |
| `2` | Partial Success | Some MIB files processed successfully, others failed |

## Limitations

### MIB Dependencies
- Requires standard SNMP MIBs (SNMPv2-SMI, SNMPv2-TC, etc.) to be available for complex MIBs
- Cannot automatically download or resolve external MIB dependencies
- Some vendor MIBs may require proprietary base MIBs

### Scope
- Only processes TRAP-TYPE and NOTIFICATION-TYPE definitions  
- Does not generate data collection configurations (use OpenNMS MIB Compiler for that)
- Does not validate generated events against OpenNMS schema

### Compatibility
- Generated events are compatible with OpenNMS 25.0.0 and later
- Uses OpenNMS event XML format - may need adjustment for other NMS platforms

## Architecture

### Technology Stack
- **Java 11+**: Runtime platform
- **JSMIParser**: MIB parsing engine  
- **PicoCLI**: Command-line interface framework
- **JAXB**: XML serialization
- **Maven**: Build system
- **SLF4J + Logback**: Logging

### Key Components
- `MibToEventsParser`: Core MIB processing and event generation
- `MibCompilerCLI`: Command-line interface and application logic
- Event model classes: JAXB-annotated POJOs for XML generation
- `ProblemEventHandler`: Error reporting and dependency tracking

## Contributing

This is a standalone extraction from the OpenNMS project. For issues or enhancements:

1. Test with the included validation examples
2. Report issues with specific MIB files and error messages  
3. Include verbose output (`-v` flag) when reporting problems
4. Provide sample MIB files that demonstrate issues (ensure no proprietary content)

## License

This project inherits the OpenNMS license structure. See the original OpenNMS project for license details.

## Acknowledgments

- Based on OpenNMS MIB Compiler functionality
- Uses JSMIParser for MIB parsing
- Extracted and adapted for standalone use