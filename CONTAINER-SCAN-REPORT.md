## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/crypto:sast
<br/><strong>Run date:</strong> Wed Jan 29 15:45:33 CET 2025
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/crypto/-/jobs/491145">Job</a>
<br/><strong>CVE founded:</strong> 1
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2024-12085](https://access.redhat.com/errata/RHSA-2025:0324)|A flaw was found in the rsync daemon which could be triggered when rsync compares file checksums. This flaw allows an attacker to manipulate the checksum length (s2length) to cause a comparison between a checksum and uninitialized memory and leak one byte of uninitialized stack data at a time.|High|Upgrade rsync to 3.1.3-20.el8_10|
