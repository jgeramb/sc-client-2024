<a target="_blank" rel="noopener noreferrer" href="https://www.software-challenge.de/">
   <p align="center">
      <img width="128" src="https://software-challenge.de/site/themes/freebird/img/logo.png" alt="Software-Challenge Germany Logo">
   </p>
</a>

# Java Client for the Software-Challenge Germany 2024

![maven](https://github.com/jgeramb/software-challenge-client/actions/workflows/maven.yml/badge.svg)

## Starting the client

The client can either be started using the provided run configurations for IntelliJ IDEA or by running the JAR file.

### Bundling the client into a JAR file

To bundle the client into a JAR file, run the following command:

```bash
mvn clean package
```

This will create a JAR file in the `target` directory.

### Running the JAR file

To run the JAR file, execute the following command:

```bash
java -jar target/teamgruen-player.jar
```

### Start arguments

The client can be started with the following arguments:

<table>
    <thead>
        <tr>
            <th>Argument</th>
            <th>Description</th>
            <th>Default</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><code>--batch-mode</code> or <code>-b</code></td>
            <td>Whether to run the client in batch (console) mode, reducing the logs to non-colored text</td>
            <td><code>false</code></td>
        </tr>
        <tr>
            <td><code>--debug</code> or <code>-d</code></td>
            <td>Whether to run the client in debug mode, enabling a closer look at the timings and moves of the player.</td>
            <td><code>false</code></td>
        </tr>
        <tr>
            <td><code>--play-style</code> or <code>-s</code></td>
            <td>The play style of the client. Allowed values are <code>simple</code> and <code>advanced</code>. This argument must be set when the client is not run in administrator/mass-testing mode.</td>
            <td>/</td>
        </tr>
        <tr>
            <td><code>--game-type</code> or <code>-g</code></td>
            <td>The game type of the room which the player should join.</td>
            <td>/</td>
        </tr>
        <tr>
            <td><code>--room</code> or <code>-r</code></td>
            <td>The ID of the room which the player should join.</td>
            <td>/</td>
        </tr>
        <tr>
            <td><code>--reservation</code> or <code>-R</code></td>
            <td>The reservation ID of the room which the player should join.</td>
            <td>/</td>
        </tr>
        <tr>
            <td><code>--host</code> or <code>-h</code></td>
            <td>The hostname of the server.</td>
            <td><code>localhost</code></td>
        </tr>
        <tr>
            <td><code>--port</code> or <code>-p</code></td>
            <td>The port of the server.</td>
            <td><code>13050</code></td>
        </tr>
        <tr>
            <td><code>--password</code></td>
            <td>The administrator password for the server.</td>
            <td><code>password</code></td>
        </tr>
    </tbody>
</table>