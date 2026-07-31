# Alpha 5 source bootstrap

These numbered files are consecutive pieces of a Base64-encoded `tar.gz` archive containing the prepared Civic Nations Alpha 5 source tree.

GitHub Actions concatenates the files in lexical order, decodes them, verifies the archive, and builds the reconstructed Gradle project.

Archive SHA-256:

`dbadde07a129a397b76db48203ebb321ca4883a20926aea4e471aaafbbddd630`

This is a temporary lossless import mechanism. It preserves every Java/resource file and the binary textures while the baseline CI build is stabilized. Once the source compiles reliably, the archive will be expanded into normal repository paths.
