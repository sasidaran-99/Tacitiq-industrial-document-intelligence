/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brandBg: "#F8FAFC",       // Warm white background
        brandText: "#0F172A",     // Slate 900 primary text
        brandEmerald: "#10B981",  // Primary Emerald
        brandAzure: "#0EA5E9",   // Secondary Azure
        brandMint: "#6EE7B7",    // Accent Mint
        brandSky: "#38BDF8",     // Support Sky Blue
        
        // Map older variables to maintain code compatibility and clean transitions
        darkBg: "#F8FAFC",
        darkPanel: "rgba(255, 255, 255, 0.78)",
        darkBorder: "rgba(255, 255, 255, 0.55)",
        brandBlue: "#0EA5E9",    // Map blue to Azure
        brandGreen: "#10B981",   // Map green to Emerald
        brandAmber: "#F59E0B",   // Vibrant Amber
        brandRed: "#EF4444",     // Vibrant Red
        brandGray: "#64748B",    // Steel Gray
      },
      boxShadow: {
        'glass': '0 8px 32px 0 rgba(15, 23, 42, 0.04)',
        'premium': '0 20px 40px -15px rgba(15, 23, 42, 0.05), 0 1px 3px rgba(15, 23, 42, 0.02)',
        'hover-glow': '0 12px 30px -10px rgba(16, 185, 129, 0.2)'
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"]
      }
    },
  },
  plugins: [],
}
