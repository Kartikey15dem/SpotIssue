import SwiftUI
import Shared

struct OtpScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getAuthViewModel() }
    @EnvironmentObject var router: Router
    
    @State private var focusedIndex = 0

    var body: some View {
        Observing(holder.vm.uiState) { (state: AuthUiState) in
            VStack(alignment: .center) {
                Spacer().frame(height: IssueSpotSpacing.huge * 2)
                
                Text("Enter verification code")
                    .font(IssueSpotTypography.headlineMedium)
                    .fontWeight(.bold)
                    .foregroundColor(IssueSpotColors.onSurface)
                
                Spacer().frame(height: IssueSpotSpacing.small)
                
                Text("We have sent you a 6 digit verification\ncode to \(state.email)")
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                
                Spacer().frame(height: IssueSpotSpacing.huge)
                
                // Native-like OTP Input
                OtpInputField(otp: Binding(
                    get: { state.otp },
                    set: { holder.vm.handleIntent(intent: AuthIntent.OtpChanged(otp: $0)) }
                ), isLoading: state.isLoading)
                .frame(height: 56)
                .padding(.horizontal, IssueSpotSpacing.medium)
                
                Spacer().frame(height: IssueSpotSpacing.extraLarge)
                
                Button(action: { holder.vm.handleIntent(intent: AuthIntent.VerifyOtpClicked.shared) }) {
                    ZStack {
                        if state.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.onPrimary))
                        } else {
                            Text("Verify OTP")
                                .font(IssueSpotTypography.labelLarge)
                                .fontWeight(.bold)
                                .foregroundColor(IssueSpotColors.onPrimary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(state.otp.count == 6 && !state.isLoading ? IssueSpotColors.primary : IssueSpotColors.primary.opacity(0.5))
                    .cornerRadius(12)
                }
                .disabled(state.otp.count != 6 || state.isLoading)
                
                Spacer().frame(height: IssueSpotSpacing.medium)
                
                Button(action: { holder.vm.handleIntent(intent: AuthIntent.SendOtpClicked.shared) }) {
                    Text("Resend Code")
                        .font(IssueSpotTypography.labelLarge)
                        .foregroundColor(IssueSpotColors.primary)
                }
                .disabled(state.isLoading)
                
                Spacer()
            }
            .padding(IssueSpotSpacing.large)
            .background(IssueSpotColors.surface.ignoresSafeArea())
            .navigationBarHidden(false) // Let user go back
            .task {
                for await effect in holder.vm.effect {
                    switch effect {
                    case let nav as AuthEffect.NavigateToNameCaptureScreen:
                        router.navigate(to: .nameCapture(nav.email))
                    case let errorEffect as AuthEffect.ShowSnackbar:
                        SnackbarManager.shared.show(errorEffect.message)
                    default:
                        break
                    }
                }
            }
        }
    }
}

struct OtpInputField: View {
    @Binding var otp: String
    let isLoading: Bool
    @FocusState private var isFocused: Bool
    
    var body: some View {
        ZStack {
            // Styled Boxes (Background)
            HStack(spacing: IssueSpotSpacing.smallMedium) {
                ForEach(0..<6, id: \.self) { index in
                    let char = otp.count > index ? String(Array(otp)[index]) : ""
                    
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(isFocused && otp.count == index ? IssueSpotColors.primary : (char.isEmpty ? IssueSpotColors.outline : IssueSpotColors.primary), lineWidth: 2)
                            .background(IssueSpotColors.surface)
                        
                        if char.isEmpty {
                            if isFocused && otp.count == index {
                                Rectangle()
                                    .fill(IssueSpotColors.primary)
                                    .frame(width: 2)
                                    .padding(.vertical, 8)
                            } else {
                                Circle()
                                    .fill(IssueSpotColors.outline)
                                    .frame(width: 8, height: 8)
                            }
                        } else {
                            Text(char)
                                .font(IssueSpotTypography.bodyLarge)
                                .fontWeight(.bold)
                                .foregroundColor(IssueSpotColors.onSurface)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            
            // Invisible but Focusable TextField
            TextField("", text: $otp)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($isFocused)
                .accentColor(.clear) // Hide native cursor
                .foregroundColor(.clear) // Hide text
                .disableAutocorrection(true)
                .onChange(of: otp) { _, newValue in
                    let filtered = newValue.filter { "0123456789".contains($0) }
                    if filtered.count > 6 {
                        otp = String(filtered.prefix(6))
                    } else {
                        otp = filtered
                    }
                }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            isFocused = true
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                isFocused = true
            }
        }
    }
}
