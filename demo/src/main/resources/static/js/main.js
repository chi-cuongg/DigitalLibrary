/**
 * Main JavaScript for Digital Library Landing Page
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Initialize AOS Animation Library with fallback
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            easing: 'ease-in-out',
            once: true,
            offset: 50
        });
    } else {
        console.warn('AOS library not loaded. Falling back to default visibility.');
        document.querySelectorAll('[data-aos]').forEach(el => {
            el.style.opacity = '1';
            el.style.transform = 'none';
        });
    }

    // 2. Sticky Navbar & Scroll Effects
    const navbar = document.getElementById('navbar');
    
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });

    // 3. Ripple Effect for Buttons
    const rippleButtons = document.querySelectorAll('.ripple-effect');
    rippleButtons.forEach(btn => {
        btn.addEventListener('mousedown', function (e) {
            let x = e.clientX - e.target.getBoundingClientRect().left;
            let y = e.clientY - e.target.getBoundingClientRect().top;
            
            let ripples = document.createElement('span');
            ripples.style.left = x + 'px';
            ripples.style.top = y + 'px';
            ripples.style.position = 'absolute';
            ripples.style.background = 'rgba(255, 255, 255, 0.3)';
            ripples.style.transform = 'translate(-50%, -50%)';
            ripples.style.pointerEvents = 'none';
            ripples.style.borderRadius = '50%';
            ripples.style.animation = 'animateRipple 1s linear';
            
            this.appendChild(ripples);
            
            setTimeout(() => {
                ripples.remove();
            }, 1000);
        });
    });

    // 4. Statistics Counter Animation
    const counters = document.querySelectorAll('.counter');
    let hasAnimated = false;

    const animateCounters = () => {
        counters.forEach(counter => {
            const target = +counter.getAttribute('data-target');
            const duration = 2000; // ms
            const increment = target / (duration / 16); // 60fps
            
            let current = 0;
            const updateCounter = () => {
                current += increment;
                if (current < target) {
                    counter.innerText = Math.ceil(current).toLocaleString();
                    requestAnimationFrame(updateCounter);
                } else {
                    counter.innerText = target.toLocaleString();
                    if(target > 1000) counter.innerText += '+';
                }
            };
            updateCounter();
        });
    };

    // Use Intersection Observer for counters
    const statsSection = document.querySelector('.statistics-section');
    if (statsSection) {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && !hasAnimated) {
                animateCounters();
                hasAnimated = true;
            }
        }, { threshold: 0.5 });
        
        observer.observe(statsSection);
    }

    // 5. Mild Parallax on Mouse Move (Hero Section)
    const heroSection = document.querySelector('.fullscreen-hero');
    const floatingShapes = document.querySelectorAll('.shape');
    
    if (heroSection) {
        heroSection.addEventListener('mousemove', (e) => {
            // Calculate mouse position relative to center of screen
            const x = e.clientX / window.innerWidth;
            const y = e.clientY / window.innerHeight;
            
            floatingShapes.forEach((shape, index) => {
                const speed = (index + 1) * 20;
                const xOffset = (window.innerWidth / 2 - e.pageX) / speed;
                const yOffset = (window.innerHeight / 2 - e.pageY) / speed;
                shape.style.transform = `translate(${xOffset}px, ${yOffset}px)`;
            });
        });
        
        // Reset when mouse leaves
        heroSection.addEventListener('mouseleave', () => {
            floatingShapes.forEach(shape => {
                shape.style.transform = 'translate(0, 0)';
            });
        });
    }
});

// Add dynamic ripple keyframe to document head
const style = document.createElement('style');
style.innerHTML = `
    @keyframes animateRipple {
        0% { width: 0; height: 0; opacity: 0.5; }
        100% { width: 500px; height: 500px; opacity: 0; }
    }
`;
document.head.appendChild(style);
