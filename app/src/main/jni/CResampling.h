/*
	File..........:	CResampling.h
	Function......:	Provides basic functions for 2x up- and
					downsampling
	Author........:	Marco Ruhland
					Institut fuer Hoertechnik+Audiologie
					Oldenburg
	Date..........:	10.03.2009
	Last changes..:	10.03.2009
	Licence.......: see end of file
*/

#ifndef IHAB_RL_CRESAMPLING_H
#define IHAB_RL_CRESAMPLING_H

class CResampling{
public:

	/* Constructor: CResampling()  */
	/* Input......: -/-            */
	/* Output.....: -/-            */
	/* Comment....: Sets up filter coeffs and state memories for */
	/*				anti-alias lowpass filter                    */
	CResampling();

	/* Destructor.: ~CResampling() */
	/* Input......: -/-            */
	/* Output.....: -/-            */
	/* Comment....: Deletes coeff and state memories */
    virtual ~CResampling();

	/* Function...: reset()        */
	/* Input......: -/-            */
	/* Output.....: -/-            */
	/* Comment....: Resets the state memories */
    void reset();

	/* Function...: Downsample2f(...)                */
	/* Input......: float* inbuf, int numoutsamples  */
	/* Output.....: float* outbuf                    */
	/* Comment....: Performs anti-alias filtering at nyquist frequency    */
	/*				and downsampling by factor 2. Note that numoutsamples */
	/*				denotes the number of output samples, this is the     */
	/*				number of input samples divided by 2.                 */
    void Downsample2f(float* inbuf,float* outbuf,int numoutsamples);

	/* Function...: Downsample2fnoLP(...)            */
	/* Input......: float* inbuf, int numoutsamples  */
	/* Output.....: float* outbuf                    */
	/* Comment....: Performs downsampling by factor 2 by simply throwing  */
	/*              every 2nd sample away. No anti-alias filtering is     */
	/*              done. This suffices for oversampling tasks.           */
	/*              Note that numoutsamples denotes the number of output  */
	/*				samples, this is the number of input samples divided  */
	/*              by 2.                                                 */
	void Downsample2fnoLP(float* inbuf,float* outbuf,int numoutsamples);

	/* Function...: Upsample2f(...)		             */
	/* Input......: float* inbuf, int numinsamples   */
	/* Output.....: float* outbuf                    */
	/* Comment....: Performs upsampling by factor 2 and anti-alias        */
	/*				filtering at nyquist frequency.                       */
	/*              Note that numinsamples denotes the number of input    */
	/*				samples for upsampling, that means that outbuf must   */
	/*				be big enough to hold the double number of samples.   */
    void Upsample2f(float* inbuf,float* outbuf,int numinsamples);

private:
	int m_xpos1_ds;
	int m_xpos2_ds;
	int m_xpos_us;
	float* m_bcoeff_ds;
	float* m_xvec1_ds;
	float* m_xvec2_ds;
	float* m_bcoeff_us;
	float* m_xvec_us;
};

#endif

/*
Copyright (c) 2009 Marco Ruhland, IHA Oldenburg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

/* END OF FILE */