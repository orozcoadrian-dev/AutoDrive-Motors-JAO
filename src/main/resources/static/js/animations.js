"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page !== "inicio") return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    if (!window.gsap || !window.ScrollTrigger) return;

    const { gsap, ScrollTrigger } = window;
    gsap.registerPlugin(ScrollTrigger);

    const hero = document.querySelector(".hero-autodrive");
    const imagen = document.querySelector(".hero-autodrive__art img");
    if (!hero || !imagen) return;

    gsap.timeline({ defaults: { ease: "power2.out" } })
        .from(imagen, { duration: 1, opacity: 0, scale: 1.04 })
        .from("[data-hero-enter]", { duration: .55, y: 18, opacity: 0, stagger: .1 }, "-=.5");

    const mm = gsap.matchMedia();

    mm.add("(min-width: 1024px)", () => {
        gsap.to(imagen, {
            yPercent: 8,
            ease: "none",
            scrollTrigger: { trigger: hero, start: "top top", end: "bottom top", scrub: true }
        });
        gsap.to(".carretera__pista", {
            yPercent: -8,
            ease: "none",
            scrollTrigger: { trigger: ".carretera", start: "top bottom", end: "bottom top", scrub: true }
        });
    });

    mm.add("(min-width: 600px)", () => {
        [".metricas", ".composicion-categorias", ".pilares"].forEach((selector) => {
            const grupo = document.querySelector(selector);
            if (!grupo) return;
            gsap.from(grupo.children, {
                y: 20,
                opacity: 0,
                duration: .5,
                stagger: .08,
                ease: "power1.out",
                scrollTrigger: { trigger: grupo, start: "top 85%", once: true }
            });
        });
    });

    window.addEventListener("pagehide", () => mm.revert(), { once: true });
});
