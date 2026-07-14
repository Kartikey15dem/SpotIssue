# IssueSpot

A cross-platform mobile application that empowers users to highlight, track, and engage with community issues. Built with **Kotlin Multiplatform (KMP)**, the application maximizes code sharing up to the ViewModel layer while leveraging the full power of native UI frameworks (**Jetpack Compose** for Android and **SwiftUI** for iOS) for an uncompromised user experience.

## 📱 Overview

IssueSpot provides a structured platform for civic engagement and issue reporting. Users can post issues utilizing rich media (Text, Images, Videos, PDFs) and categorize them by geographical impact. 

The core feed relies on a dynamic, community-driven escalation system across four distinct scopes:
* **Locality** 
* **District**
* **State** 
* **National**

As localized issues gain traction and receive "acknowledgements" from the community, they automatically escalate to higher geographical tiers, ensuring critical problems get the broader visibility they deserve.

## ✨ Key Features

* **Community-Driven Escalation:** Posts organically grow in reach. An issue posted at the Locality level will automatically be promoted to the District, State, and eventually National feeds as its acknowledgement count increases.
* **Secure Authentication:** Passwordless entry utilizing Email OTP verification.
* **Multi-Tiered Feed:** Four dedicated tabs to seamlessly filter and view issues based on your current geographical scope.
* **Rich Media Issue Reporting:** Users can create posts detailing issues with text, image, video, and PDF attachments.
* **Advanced Profile Management:** 
    * View isolated feeds of *My Posts* and *Liked Posts*.
    * Deep-sorting capabilities: Sub-sort feeds by *Popular*, *Oldest*, and *Latest*.
* **High-Performance Offline-First Pagination:** 
    * Custom pagination architecture built from scratch to bypass common Paging3 bugs (e.g., premature pagination termination and list jumping).
    * Implements a custom presentation cache layer (similar to DiffUtil) bridging Compose and SwiftUI to ensure smooth, jitter-free scrolling even on slow networks.

## 🏗 Architecture & Tech Stack

This project follows a strict **Shared UI-State / Native UI-Rendering** paradigm.

### Shared Code (Kotlin Multiplatform)
* **Domain & Data Layers:** Repository patterns, local database caching, and network calls.
* **Presentation Layer:** Shared KMP ViewModels managing state and business logic.
* **Pagination:** Custom offline-first caching and pagination strategy (inspired by [this architecture](https://medium.com/@kgbduswns/android-high-performance-offline-first-paging-architecture-without-paging3-2bd23a61f8f3)).

### Native UI Implementation
* **Android:** Jetpack Compose natively observing shared ViewModels.
* **iOS:** SwiftUI utilizing **SKIE** to seamlessly bridge KMP Coroutines and StateFlows directly into Swift's modern Observing API.

## 📱 App Previews

The SwiftUI iOS implementation is in progress

| Android |
| :---: | 
| <video src="https://github.com/user-attachments/assets/edd2ff09-0d15-47be-b132-98ed250b36fe" controls height="200"></video> 

[Spring Boot Backend Repository](https://github.com/Kartikey15dem/IssueSpot-backend)**
---
