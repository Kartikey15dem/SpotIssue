import SwiftUI
import Shared
import CoreLocation

struct LocationFetchScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getLocationFetchViewModel() }
    @EnvironmentObject var router: MainRouter
    @State private var showRationaleDialog = false

    var body: some View {
        Observing(holder.vm.uiState) { state in
            VStack(alignment: .center, spacing: IssueSpotSpacing.large) {
                
                Spacer()
                
                // Animation Area
                ZStack {
                    if state.currentStep == Shared.LocationFetchStep.fetching {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                            .scaleEffect(2)
                            .frame(width: 150, height: 150)
                    } else if state.currentStep == Shared.LocationFetchStep.completed {
                        Image(systemName: "checkmark.circle.fill")
                            .resizable()
                            .foregroundColor(IssueSpotColors.primary)
                            .frame(width: 100, height: 100)
                    } else if state.currentStep == Shared.LocationFetchStep.error {
                        Image(systemName: "location.slash.fill")
                            .resizable()
                            .foregroundColor(IssueSpotColors.error)
                            .frame(width: 100, height: 100)
                            .padding()
                            .background(IssueSpotColors.surfaceVariant)
                            .cornerRadius(16)
                    }
                }
                .frame(width: 200, height: 200)
                
                // Title
                if state.currentStep != Shared.LocationFetchStep.error {
                    Text(state.currentStep == Shared.LocationFetchStep.fetching ? "Fetching your location..." : "Location fetched successfully!")
                        .font(IssueSpotTypography.titleLarge)
                        .foregroundColor(IssueSpotColors.onSurface)
                        .multilineTextAlignment(.center)
                }
                
                // Content
                if state.currentStep == Shared.LocationFetchStep.completed, let address = state.address {
                    Text(address)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                } else if state.currentStep == Shared.LocationFetchStep.error, let errorState = state.errorState {
                    Text(errorState.message)
                        .font(IssueSpotTypography.bodyLarge)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Spacer().frame(height: IssueSpotSpacing.medium)
                    
                    Button(action: { holder.vm.handleIntent(intent: LocationFetchIntent.ActionClicked.shared) }) {
                        Text(errorState.primaryButtonText)
                            .font(IssueSpotTypography.labelLarge)
                            .fontWeight(.bold)
                            .foregroundColor(IssueSpotColors.onPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(IssueSpotColors.primary)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal)
                    
                    if errorState.showSecondaryRetry {
                        Button(action: { holder.vm.handleIntent(intent: LocationFetchIntent.RetryClicked.shared) }) {
                            Text("I've turned it on, Retry")
                                .font(IssueSpotTypography.labelLarge)
                                .fontWeight(.bold)
                                .foregroundColor(IssueSpotColors.primary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(IssueSpotColors.primary, lineWidth: 1)
                                )
                        }
                        .padding(.horizontal)
                    }
                    
                    Spacer().frame(height: IssueSpotSpacing.medium)
                    
                    Button(action: { showRationaleDialog = true }) {
                        Text("See why location is required")
                            .font(IssueSpotTypography.labelLarge)
                            .fontWeight(.semibold)
                            .foregroundColor(IssueSpotColors.primary)
                            .underline()
                    }
                    .alert("Why is Location Important?", isPresented: $showRationaleDialog) {
                        Button("Back", role: .cancel) { }
                    } message: {
                        Text("Location is required for fetching posts around you and for creating new posts, which is the primary focus of this app.")
                    }
                }
                
                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(IssueSpotColors.surface.ignoresSafeArea())
            .navigationBarHidden(true)
            .task {
                // Wait for navigation transition to finish before prompting for permission
                if state.currentStep == Shared.LocationFetchStep.fetching {
                    try? await Task.sleep(nanoseconds: 500_000_000)
                    holder.vm.handleIntent(intent: LocationFetchIntent.StartLocationFlow.shared)
                }
            }
            .task {
                for await effect in holder.vm.effect {
                    switch effect {
                    case is LocationFetchEffect.NavigateToNextScreen:
                        if state.isCompleted {
                            try? await Task.sleep(nanoseconds: 1_500_000_000)
                        }
                        router.clearAndNavigate(to: .home)
                    case is LocationFetchEffect.OpenAppSettings:
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            await UIApplication.shared.open(url)
                        }
                    case is LocationFetchEffect.PromptGpsSettings:
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            await UIApplication.shared.open(url)
                        }
                    default:
                        break
                    }
                }
            }
        }
    }
}
