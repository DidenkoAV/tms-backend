package com.test.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates HTML templates for emails and web pages.
 */
@Service
@RequiredArgsConstructor
public class HtmlTemplateService {

    @Value("${app.public-base-url}")
    private String appPublicBaseUrl;

    // ========================================================================
    // Email Templates
    // ========================================================================

    /**
     * Renders email verification template.
     */
    public String emailVerificationEmail(String fullName, String link) {
      return renderModernEmail(
          fullName,
          "Verify Your Email",
          "Welcome to Messagepoint TMS! Please verify your email address to get started.",
          "Verify Email Address",
          link,
          "This link will expire in 24 hours. If you didn't create an account, you can safely ignore this email."
      );
    }

  private String renderModernEmail(String fullName, String title, String message, String buttonText, String buttonLink, String footer) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>%s</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f8fafc;">
            <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f8fafc;">
                <tr>
                    <td align="center" style="padding: 40px 20px;">
                        <!-- Main Container -->
                        <table role="presentation" style="width: 100%%; max-width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);">
                            
                            <!-- Header with Logo -->
                            <tr>
                                <td style="padding: 40px 40px 30px 40px; text-align: center; background: linear-gradient(135deg, #7c1a87 0%%, #6a1675 100%%); border-radius: 16px 16px 0 0;">
                                    <div style="display: inline-block; background-color: #ffffff; width: 64px; height: 64px; border-radius: 16px; padding: 12px; margin-bottom: 16px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);">
                                        <svg width="40" height="40" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                            <g transform="translate(-165 -52)" fill="#7c1a87">
                                                <path d="m189 52c13.255 0 24 10.745 24 24s-10.745 24-24 24-24-10.745-24-24 10.745-24 24-24zm13 11l-12.792 9.8003-13.208-9.7817v25.981h5.8113v-14.633l7.3971 5.5031 12.792-9.5903v-7.2798zm-6 26h6v-5h-6v5z"/>
                                            </g>
                                        </svg>
                                    </div>
                                    <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 600; letter-spacing: -0.5px;">
                                        Messagepoint <span style="font-weight: 400;">TMS</span>
                                    </h1>
                                </td>
                            </tr>
                            
                            <!-- Content -->
                            <tr>
                                <td style="padding: 40px;">
                                    <h2 style="margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 600;">
                                        Hello %s,
                                    </h2>
                                    <p style="margin: 0 0 24px 0; color: #475569; font-size: 16px; line-height: 1.6;">
                                        %s
                                    </p>
                                    
                                    <!-- Button -->
                                    <table role="presentation" style="margin: 32px 0;">
                                        <tr>
                                            <td style="border-radius: 12px; background: linear-gradient(135deg, #7c1a87 0%%, #6a1675 100%%); box-shadow: 0 4px 12px rgba(124, 26, 135, 0.3);">
                                                <a href="%s" target="_blank" style="display: inline-block; padding: 16px 32px; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; border-radius: 12px;">
                                                    %s
                                                </a>
                                            </td>
                                        </tr>
                                    </table>
                                    
                                    <!-- Alternative Link -->
                                    <div style="margin-top: 32px; padding: 16px; background-color: #f8fafc; border-radius: 8px; border-left: 4px solid #7c1a87;">
                                        <p style="margin: 0 0 8px 0; color: #64748b; font-size: 13px; font-weight: 500;">
                                            If the button doesn't work, copy and paste this link:
                                        </p>
                                        <a href="%s" style="color: #7c1a87; font-size: 13px; word-break: break-all; text-decoration: none;">
                                            %s
                                        </a>
                                    </div>
                                </td>
                            </tr>
                            
                            <!-- Footer -->
                            <tr>
                                <td style="padding: 32px 40px; background-color: #f8fafc; border-radius: 0 0 16px 16px; border-top: 1px solid #e2e8f0;">
                                    <p style="margin: 0 0 12px 0; color: #64748b; font-size: 14px; line-height: 1.5;">
                                        %s
                                    </p>
                                    <p style="margin: 0; color: #94a3b8; font-size: 12px;">
                                        © 2026 Messagepoint TMS. All rights reserved.
                                    </p>
                                </td>
                            </tr>
                            
                        </table>
                        
                        <!-- Bottom Spacing -->
                        <p style="margin: 24px 0 0 0; color: #94a3b8; font-size: 12px; text-align: center;">
                            This is an automated message, please do not reply to this email.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(
        escape(title),
        escape(fullName),
        escape(message),
        buttonLink,
        escape(buttonText),
        buttonLink,
        buttonLink,
        escape(footer)
    );
  }

  private String escape(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

    /**
     * Renders password reset template.
     */
    public String passwordResetEmail(String fullName, String link) {
        return renderModernEmail(
                fullName,
                "Reset Your Password",
                "You requested to reset your password. Click the button below to set a new one.",
                "Set New Password",
                link,
                "This link will expire in 2 hours. If you didn't request this, you can safely ignore this email."
        );
    }

    /**
     * Renders email change confirmation template.
     */
    public String emailChangeConfirmEmail(String fullName, String link) {
        return renderModernEmail(
                fullName,
                "Confirm Your New Email",
                "Please confirm this new email address to complete the change for your Messagepoint TMS account.",
                "Confirm New Email",
                link,
                "This link will expire in 24 hours. If you didn't request this change, you can safely ignore this email."
        );
    }

    /**
     * Renders group invitation template.
     */
    public String groupInviteEmail(String inviteeName, String groupName, String inviterName, String link) {
        return renderModernEmail(
                inviteeName,
                "Group Invitation",
                "%s invited you to join the group \"%s\" on Messagepoint TMS. Click the button below to accept the invitation."
                        .formatted(escape(inviterName), escape(groupName)),
                "Accept Invitation",
                link,
                "This link will expire in 72 hours. If you weren't expecting this invitation, you can safely ignore this email."
        );
    }

    // ========================================================================
    // Web Page Templates
    // ========================================================================

  /**
   * Renders email verification success page.
   */
  public String emailVerificationSuccess() {
    String loginUrl = appPublicBaseUrl;
    return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1"/>
              <title>Email verified • Messagepoint TMS</title>
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { height: 100%%; }
                body {
                  margin: 0;
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                  background: linear-gradient(to bottom right, #f8fafc, #ffffff, rgba(249, 115, 22, 0.05));
                  color: #0f172a;
                  display: grid;
                  place-items: center;
                  padding: 24px;
                  min-height: 100vh;
                }
                .card {
                  width: 100%%;
                  max-width: 448px;
                  background: #ffffff;
                  border: 1px solid #e2e8f0;
                  border-radius: 16px;
                  padding: 32px;
                  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
                  animation: fadeIn 0.4s ease-out;
                }
                @keyframes fadeIn {
                  from { opacity: 0; transform: translateY(10px); }
                  to { opacity: 1; transform: translateY(0); }
                }
                .logo-container {
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  gap: 12px;
                  margin-bottom: 24px;
                }
                .logo {
                  display: inline-grid;
                  place-items: center;
                  width: 72px;
                  height: 72px;
                  background: #ffffff;
                  border-radius: 16px;
                  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                  border: 1px solid #e2e8f0;
                  transition: transform 0.3s;
                }
                .logo:hover {
                  transform: scale(1.05);
                }
                .logo svg {
                  width: 54px;
                  height: 54px;
                }
                .brand {
                  font-size: 20px;
                  font-weight: 600;
                  color: #0f172a;
                  letter-spacing: -0.025em;
                }
                .tagline {
                  font-size: 14px;
                  color: #475569;
                }
                .icon-wrapper {
                  display: flex;
                  justify-content: center;
                  margin-bottom: 24px;
                }
                .icon {
                  width: 64px;
                  height: 64px;
                  background: #7c1a87;
                  border-radius: 50%%;
                  display: grid;
                  place-items: center;
                }
                .icon svg {
                  width: 32px;
                  height: 32px;
                  color: #ffffff;
                  stroke-width: 3;
                }
                h1 {
                  margin: 0 0 12px;
                  font-size: 24px;
                  font-weight: 600;
                  color: #0f172a;
                  text-align: center;
                  letter-spacing: -0.025em;
                }
                p {
                  margin: 0 0 24px;
                  color: #475569;
                  text-align: center;
                  font-size: 16px;
                  line-height: 1.5;
                }
                .actions {
                  display: flex;
                  flex-direction: column;
                  gap: 12px;
                }
                .btn {
                  appearance: none;
                  border: none;
                  border-radius: 8px;
                  padding: 12px 24px;
                  font-weight: 600;
                  font-size: 15px;
                  cursor: pointer;
                  text-decoration: none;
                  text-align: center;
                  transition: all 0.2s;
                  display: block;
                  width: 100%%;
                }
                .primary {
                  background: #7c1a87;
                  color: #ffffff;
                }
                .primary:hover {
                  background: #6a1675;
                }
                .ghost {
                  background: #ffffff;
                  color: #475569;
                  border: 2px solid #7c1a87;
                }
                .ghost:hover {
                  background: #f8fafc;
                }
                .footer {
                  margin-top: 24px;
                  padding-top: 16px;
                  font-size: 12px;
                  color: #64748b;
                  text-align: center;
                  line-height: 1.5;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo-container">
                  <div class="logo">
                    <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                      <g transform="translate(-165 -52)" fill="#7c1a87">
                        <path d="m189 52c13.255 0 24 10.745 24 24s-10.745 24-24 24-24-10.745-24-24 10.745-24 24-24zm13 11l-12.792 9.8003-13.208-9.7817v25.981h5.8113v-14.633l7.3971 5.5031 12.792-9.5903v-7.2798zm-6 26h6v-5h-6v5z"/>
                      </g>
                    </svg>
                  </div>
                  <div class="brand">Messagepoint TMS</div>
                  <div class="tagline">Your smart test workspace</div>
                </div>
                
                <div class="icon-wrapper">
                  <div class="icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                    </svg>
                  </div>
                </div>
                
                <h1>Email Verified!</h1>
                <p>Your email has been verified successfully. You can now sign in to your account.</p>
                
                <div class="actions">
                  <a class="btn primary" href="%s">Go to Login</a>
                  <a class="btn ghost" href="/">Back to Home</a>
                </div>
                
                <div class="footer">
                  If this wasn't you, you can safely ignore this page.
                </div>
              </div>
            </body>
            </html>
            """.formatted(escape(loginUrl));
  }

    /**
     * Renders email verification error page.
     */
    public String emailVerificationError() {
        String loginUrl = appPublicBaseUrl + "/login";
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                  <title>Verification link invalid • TMS AI</title>
                  <style>
                    :root { --bg:#0b0f1a; --card:#0f1524; --muted:#94a3b8; --text:#e5e7eb; }
                    html,body{height:100%%;}
                    body{margin:0;background:var(--bg);color:var(--text);font-family:Inter,system-ui,Segoe UI,Arial,sans-serif;}
                    .bar{height:2px;background:linear-gradient(90deg,#22d3ee,#8b5cf6,#22d3ee);opacity:.7}
                    .wrap{min-height:calc(100vh - 2px);display:grid;place-items:center;padding:24px}
                    .card{max-width:560px;width:100%%;background:color-mix(in srgb, var(--card) 95%%, transparent);
                          border:1px solid #1f2937;border-radius:16px;padding:28px;position:relative;
                          box-shadow:0 12px 40px rgba(0,0,0,.45)}
                    .glow{position:absolute;inset:-1px;border-radius:16px;pointer-events:none;
                          background:linear-gradient(90deg,#ef444455,#f59e0b55,#ef444455);mask:linear-gradient(#000,#000) content-box,linear-gradient(#000,#000);
                          -webkit-mask-composite: xor;mask-composite:exclude;padding:1px;opacity:.6}
                    .icon{width:64px;height:64px;margin:8px auto 16px;border-radius:999px;
                          display:grid;place-items:center;border:1px solid #7f1d1d;
                          background:radial-gradient(80%% 80%% at 50%% 20%%,#ef444433,transparent)}
                    h1{margin:0 0 8px;font-size:26px;letter-spacing:.2px;color:#fecaca}
                    p{margin:6px 0 0;color:var(--muted)}
                    .actions{margin-top:18px;display:flex;gap:10px;flex-wrap:wrap}
                    .btn{appearance:none;border:0;border-radius:10px;padding:10px 16px;font-weight:600;cursor:pointer;text-decoration:none}
                    .primary{background:linear-gradient(90deg,#22d3ee,#8b5cf6);color:#000}
                    .ghost{background:#0b1222;color:#cbd5e1;border:1px solid #1f2937}
                    .footer{margin-top:16px;font-size:12px;color:#64748b}
                  </style>
                </head>
                <body>
                  <div class="bar"></div>
                  <div class="wrap">
                    <div class="card">
                      <div class="glow"></div>
                      <div class="icon">⚠️</div>
                      <h1>Verification link is invalid or expired</h1>
                      <p>Please request a new confirmation email and try again.</p>
                      <div class="actions">
                        <a class="btn primary" href="%s">Go to Login</a>
                        <a class="btn ghost" href="/">Back to site</a>
                      </div>
                      <div class="footer">The previous link may have been used already or expired.</div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escape(loginUrl));
    }

}

