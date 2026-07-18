import SwiftUI
import Shared

struct AuthScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getAuthViewModel() }
    @EnvironmentObject var router: AuthRouter
    
    var body: some View {
        Observing(holder.vm.uiState) { state in
            VStack(alignment: .center) {
                Spacer().frame(height: IssueSpotSpacing.huge * 2)
                
                Image("logo_issue")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 160, height: 160)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .background(IssueSpotColors.surface)
                
                Text("IssueSpot")
                    .font(IssueSpotTypography.headlineLarge)
                    .foregroundColor(IssueSpotColors.onSurface)
                    .padding(.top, IssueSpotSpacing.medium)
                
                Spacer().frame(height: IssueSpotSpacing.huge)
                
                TextField("Email Address", text: Binding(
                    get: { state.email },
                    set: { holder.vm.handleIntent(intent: AuthIntent.EmailChanged(email: $0)) }
                ))
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .font(IssueSpotTypography.bodyLarge)
                .padding()
                .frame(height: 56)
                .background(IssueSpotColors.surface)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(IssueSpotColors.outline, lineWidth: 1)
                )
                .disabled(state.isLoading)
                
                Spacer().frame(height: IssueSpotSpacing.large)
                
                Text("We will use your email address for verification\npurpose. An OTP will be sent to your email.")
                    .font(IssueSpotTypography.bodySmall)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                
                Spacer().frame(height: IssueSpotSpacing.extraLarge)
                
                let isEmailValid = state.email.contains("@") && state.email.contains(".")
                
                Button(action: { holder.vm.handleIntent(intent: AuthIntent.SendOtpClicked.shared) }) {
                    ZStack {
                        if state.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.onPrimary))
                        } else {
                            Text("Login / Sign up")
                                .font(IssueSpotTypography.labelLarge)
                                .fontWeight(.bold)
                                .foregroundColor(IssueSpotColors.onPrimary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(isEmailValid && !state.isLoading ? IssueSpotColors.primary : IssueSpotColors.primary.opacity(0.5))
                    .cornerRadius(12)
                }
                .disabled(!isEmailValid || state.isLoading)
                
                Spacer()
            }
            .padding(IssueSpotSpacing.large)
            .background(IssueSpotColors.surface.ignoresSafeArea())
            .navigationBarHidden(true)
            .task {
                for await effect in holder.vm.effect {
                    switch effect {
                    case let nav as AuthEffect.NavigateToOtpScreen:
                        router.navigate(to: .otp)
                    case let nav as AuthEffect.NavigateToNameCaptureScreen:
                        router.navigate(to: .nameCapture(nav.email))
                    case let errorEffect as AuthEffect.ShowDialog:
                        AppDialogManager.shared.show(errorEffect.message)
                    default:
                        break
                    }
                }
            }
        }
    }
}
