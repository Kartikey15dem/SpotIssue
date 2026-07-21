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
* **Hyper-Local Spatial Search:** While State, District, and National feeds rely on standard reverse-geocoded name matching, neighborhood boundaries are too blurry for string-matching. The "Locality" feed utilizes **PostGIS** to perform hyper-accurate spatial queries, fetching issues strictly within a 10km radius of the user's live coordinates.
* **Resilient Background Uploads & State Restoration:** Uploading large media files can easily fail if the user minimizes the app. To prevent data loss, I engineered a background upload pipeline utilizing Android **WorkManager** and iOS **Background Tasks**, coupled with KMP DataStore to automatically save and restore post drafts if an upload is interrupted or fails.
* **Dynamic Keyboard Inset Handling (Android):** Fixes a notorious bug found in many major social platforms where text extending beneath the soft keyboard gets hidden and cannot be tapped/scrolled to. The *Create Post* screen implements intelligent cursor tracking and programmatic scrolling to ensure the typing area is always in clear view.
* **Multi-Tiered Feed:** Four dedicated tabs to seamlessly filter and view issues based on your current geographical scope.
* **Rich Media Issue Reporting:** Users can create posts detailing issues with text, image, video, and PDF attachments.
* **Advanced Profile Management:** 
    * View isolated feeds of *My Posts* and *Liked Posts*.
    * Deep-sorting capabilities: Sub-sort feeds by *Popular*, *Oldest*, and *Latest*.
* **Offline-First Architecture:** Built from the ground up to function offline, leveraging **Room Database** to locally cache and reliably serve paged posts even when network connectivity is lost.
* **High-Performance Offline-First Pagination:** 
    * Custom pagination architecture built from scratch to bypass common Paging3 bugs (e.g., premature pagination termination and unprompted viewport shifting/list jumping).
    * Implements a custom presentation cache layer (similar to DiffUtil) bridging Compose and SwiftUI to ensure smooth, jitter-free scrolling even on slow networks. 
    * *Note: This solution was heavily inspired by [Yeonjun Kim's Medium article](https://medium.com/@kgbduswns/android-high-performance-offline-first-paging-architecture-without-paging3-2bd23a61f8f3).*

## 🏗 Architecture & Tech Stack

This project follows a strict **Shared UI-State / Native UI-Rendering** paradigm, backed by a robust spatial-aware backend.

### Shared Code (Kotlin Multiplatform)
* **Domain & Data Layers:** Repository patterns, local database caching, and network calls.
* **Presentation Layer:** Shared KMP ViewModels managing state and business logic.
* **Pagination:** Custom offline-first caching and pagination strategy.

### Native UI Implementation
* **Android:** Jetpack Compose natively observing shared ViewModels.
* **iOS:** SwiftUI utilizing Touchlab's **SKIE** Observing API to seamlessly bridge KMP Coroutines and StateFlows directly into Swift's modern ecosystem. *(Note: The SwiftUI UI code was generated with AI assistance).*

### Backend & Database
* **Framework:** Spring Boot
* **Database:** Supabase (PostgreSQL)
* **Spatial Processing:** PostGIS (Utilizing `geography(Point, 4326)` mappings and `ST_DWithin` functions for real-time spherical radius distance calculations).
* **Repo Link:** [IssueSpot Spring Boot Backend](https://github.com/Kartikey15dem/IssueSpot-backend)

## 📱 App Previews

| Android | iOS |
| :---: | :---: |
| <video src="https://github.com/user-attachments/assets/96868d12-f225-44e0-8fab-f8642ec789b2" controls height="400"></video> | <video src="https://github.com/user-attachments/assets/012039c4-8509-4f85-b681-ea86b42745df" controls height="400"></video> 

---

## 🐛 Bug Spotlight: Soft-Keyboard Cursor Occlusion (Android)

Many popular social applications suffer from an annoying UI bug: when typing a long post, the text eventually flows behind the soft keyboard. Tapping the bottom text doesn't push the screen up, leaving the user typing blindly. 

Standard `imePadding()` modifiers in Jetpack Compose often fail to solve this inside heavily scrollable, multi-line `BasicTextField` components. 

**The Solution:**
Instead of relying solely on window insets, IssueSpot implements a custom scroll calculation for Android. The `CreatePostScreen` tracks the cursor's exact Y-coordinate dynamically using `TextLayoutResult`. By comparing the cursor's relative screen position against the device's keyboard height (`WindowInsets.ime`), the app calculates the exact overlap. A `LaunchedEffect` then programmatically triggers `scrollState.scrollBy()` to push the viewport up, guaranteeing the active typing area always remains visible above the keyboard.

### See it in action:

| The Bug (Hidden Cursor) | The Fix (Dynamic Auto-Scroll) |
| :---: | :---: | 
| <video src="https://github.com/user-attachments/assets/3bb94201-b21a-4dd4-bf79-e7b1fcaf34ce" controls height="400"></video> | <video src="https://github.com/user-attachments/assets/4c94702f-268a-4749-a910-597a1b354d08" controls height="400"></video> 

---
