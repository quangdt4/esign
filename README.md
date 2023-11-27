# e-Signature
- Graduation project {2023}
- Document management
- Digital, e-Signature
- Following Public key infrastructure - PKI

## Install
- Update Android Studio to the newest version [Android studio here](https://developer.android.com/studio)
- Config code style (Use default Kotlin coding convention, and select use single import)
- Download Adobe Acrobat and register certification

## Conventions
- We use ktlint for code style checking
- [Kotlin style here](https://developer.android.com/kotlin/style-guide)

## Workflows
- Create digital signature's certificate on Adobe Acrobat (PKCS#12, .p12)
- Import documents (PDF)
- Sign
- Verify it with PKCS#7

### Working with GIT
- Workflows for Git: [Gitflow](https://nvie.com/posts/a-successful-git-branching-model/)
- New feature: branch out from develop/ base feature branch and name it `feature/...`
- Bug: branch out from develop/ base feature branch and name it `bugfix/...`
- When making PR, please select to ***squash commits*** and ***delete source branch*** when merge to keep our branch list clean

## Libraries
- itextpdf: [itextpdf](https://github.com/itext/itextpdf)
- PDFBox: [PDFBox](https://github.com/TomRoush/PdfBox-Android)
- BouncyCastle
- Keystore Android
- ....

## Unit tests
- We use Junit4, Mockito, MockK...

## Preview
![docs_list](https://github.com/quangdt4/esign/assets/76697615/01613b1b-36b4-409c-b4c3-70aafe9aea56)
![docs_view](https://github.com/quangdt4/esign/assets/76697615/4ed22d84-3d07-4b6c-8e36-715a87b3d4c2)
![sign](https://github.com/quangdt4/esign/assets/76697615/85afe427-4dcb-49dc-a4b8-82dd76b69dcb)
