<img src="src/main/resources/assets/sodium/icon.png" width="128">
# Sodium (for Starline Iris Fork)

This is a fork of Iris's Sodium Compatibility fork intended to be used for testing and developing with the starline branch on my Iris fork.

## Notes on compiling the fork

**You will need to install JDK 8** in order to build this fork. You can either install this through
a package manager such as [Chocolatey](https://chocolatey.org/) on Windows or [SDKMAN!](https://sdkman.io/) on other
platforms. If you'd prefer to not use a package manager, you can always grab the installers or packages directly from
[AdoptOpenJDK](https://adoptopenjdk.net/).

On Windows, the Oracle JDK/JRE builds should be avoided where possible due to their poor quality. Always prefer using
the open-source builds from AdoptOpenJDK when possible.

**You need to have a compiled build of the Iris (The-HyperZone fork) starline branch in your mavenLocal repository. You will be unable to compile this fork otherwise!** Use the `publishToMavenLocal` Gradle task in the Iris repository (checked out to the starline branch) to accomplish this.

### License

Sodium (and this fork) are licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](https://github.com/jellysquid3/sodium-fabric/blob/1.16.x/dev/LICENSE.txt).
