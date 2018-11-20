# Webcrawler Node 

Webcrawler node is a Java Project containing all the crawlers from Lett.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

#### Install Java and Eclipse
1 - Open a terminal (Ctrl-Alt-T) and switch it to root permissions by entering:

```  
$ sudo su
```

2 - Make sure Eclipse Indigo is NOT installed in your Ubuntu. You may need to remove both "eclipse" and "eclipse-platform" packages to get rid of it. If it still gets into way when trying to install Luna using this easy way, you may need to look at the "hard way" below.

```    
# apt-get remove eclipse eclipse-platform
```

3 - Install a Java 1.8 JDK:

```  
# apt-get install openjdk-8-jdk
```

4 - Install Maven:

```    
# apt-get install maven
```

5 - Get rid of the root access as you won't need it anymore:

```
# exit
```

6 - Download Eclipse Oxygen. The "for Java EE Developers" or "for Java Developers" versions all seem to work. 
Currently the file which was tested to work is (note that it is for 64 bit Ubuntu version) available at this page

7 - Extract the Eclipse installation tarball into your home directory:

```
$ cd
$ tar -xzvf <path/to/your-tar-file>
```

8 - Increase the memory for the Eclipse installation by modifying the ~/eclipse/eclipse.ini file.

9 - Change the -Xmx setting (line 20) to be AT least 1G, recommended 2GB. (i.e. -Xmx2048m).

10 - Change the -XX:MaxPermSize (line 18) to at most 512m. If you have the -Xmx setting set to 1G, then I suggest to use a lower value, for example 300m.

11 - Run Eclipse:

```
$ ~/eclipse/eclipse
```

#### Connect to VPN:

Before running the application, you need to connect to our VPN. If you do not know how to do it, talk with the Team Leader.

#### Create Configuration file:

You need to create a file named settings.xml. This file is for download maven dependencies.
To create this file, you will need AWS credentials, ask for you leader.

After that you need to add that file to your home's .m2 folder.

Settings.xml example:

```
<?xml version="1.0"?>

<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <servers>
        <server>
            <id>maven-id</id>
            <username>user</username>
            <password>pass</password>
        </server>
    </servers>
</settings>
```

#### Import project from git:

```
git clone [address]
```

#### Configure to maven project:

In your eclipse, set your project for a maven project, after that the dependencies will be downloaded.

## Launch Configurations

You can ask your team leader for this configurations, after that you need to change the credentials
of the environment variables to your credentials, like postgres, mongo and aws.

## Running the tests

https://www.notion.so/lett/Crawler-1d6ce7a0dee94de686c8f2458a3f810f


### And coding style tests

https://www.notion.so/lett/Java-Style-Guideline-512c01ec2de04d77a3af73e78e9005eb

## Deployment

Use tag [deploy:Production] in your commit for deploy changes, but this only can be done on branch master

## Authors

Fabricio Massula
Samir Leão
Gabriel Dornelas

## Acknowledgments

* REAMDE.md template from [PurpleBooth](https://gist.githubusercontent.com/PurpleBooth/109311bb0361f32d87a2/raw/8254b53ab8dcb18afc64287aaddd9e5b6059f880/README-Template.md)